/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets.AggregatedReachedSetManager;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Options(prefix = "parallelAlgorithm")
public class ParallelAlgorithm implements Algorithm {

  @Option(
    secure = true,
    required = true,
    description =
        "List of files with configurations to use. Files can be suffixed with"
            + " ::supply-reached this signalizes that the (finished) reached set"
            + " of an analysis can be used in other analyses (e.g. for invariants"
            + " computation). If you use the suffix ::supply-reached-refinable instead"
            + " this means that the reached set supplier is additionally continously"
            + " refined (so one of the analysis has to be instanceof ReachedSetAdjustingCPA)"
            + " to make this work properly."
  )
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> configFiles;

  private static final Splitter CONFIG_FILE_CONDITION_SPLITTER = Splitter.on("::").trimResults();

  private final Configuration globalConfig;
  private final LogManager logger;
  private final ShutdownManager shutdownManager;
  private final CFA cfa;
  private final String filename;

  private ParallelAnalysisResult finalResult = null;
  private CFANode mainEntryNode = null;
  private final AggregatedReachedSetManager aggregatedReachedSetManager;

  public ParallelAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      String pFilename,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException {
    config.inject(this);

    globalConfig = config;
    logger = checkNotNull(pLogger);
    shutdownManager = ShutdownManager.createWithParent(checkNotNull(pShutdownNotifier));
    cfa = checkNotNull(pCfa);
    filename = checkNotNull(pFilename);

    aggregatedReachedSetManager = new AggregatedReachedSetManager();
    aggregatedReachedSetManager.addAggregated(pAggregatedReachedSets);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    mainEntryNode = AbstractStates.extractLocation(pReachedSet.getFirstState());
    ForwardingReachedSet forwardingReachedSet = (ForwardingReachedSet) pReachedSet;

    ListeningExecutorService exec = listeningDecorator(newFixedThreadPool(configFiles.size()));
    List<ListenableFuture<ParallelAnalysisResult>> futures = new ArrayList<>();
    int counter = 1;
    for (Path p : configFiles) {
      futures.add(exec.submit(createAlgorithm(p, counter++)));
    }
    addCancellationCallback(futures);

    // shutdown the executor service,
    exec.shutdown();

    handleFutureResults(futures);

    // wait some time so that all threads are shut down and have (hopefully) finished their logging
    if (!exec.awaitTermination(10, TimeUnit.SECONDS)) {
      logger.log(Level.WARNING, "Not all threads are terminated although we have a result.");
    }

    if (finalResult != null) {
      forwardingReachedSet.setDelegate(finalResult.getReached());
      return finalResult.getStatus();
    }

    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private void handleFutureResults(List<ListenableFuture<ParallelAnalysisResult>> futures)
      throws InterruptedException, Error, CPAException {
    List<CPAException> exceptions = new ArrayList<>();
    for (ListenableFuture<ParallelAnalysisResult> f : futures) {
      try {
        ParallelAnalysisResult result = f.get();
        if (result.hasValidReachedSet() && finalResult == null) {
          finalResult = result;
        }
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        } else if (cause instanceof Error) {
          throw (Error) cause;
        } else if (cause instanceof CPAException) {
            if (cause.getMessage().contains("recursion")) {
              logger.logUserException(Level.WARNING, e, "Analysis not completed due to recursion");
            }
            if (cause.getMessage().contains("pthread_create")) {
              logger.logUserException(Level.WARNING, e, "Analysis not completed due to concurrency");
            }
            exceptions.add((CPAException) cause);
        } else {
          throw new CPAException("An unexpected exception occured", e);
        }
      } catch (CancellationException e) {
        // do nothing, this is normal if we cancel other analyses
      }
    }

    // we do not have any result, so we propagate the found CPAExceptions upwards
    if (finalResult == null && !exceptions.isEmpty()) {
      if (exceptions.size() == 1) {
        throw Iterables.getOnlyElement(exceptions);
      } else {
        throw new CompoundException("Several exceptions occured during the analysis", exceptions);
      }
    }
  }

  private void addCancellationCallback(List<ListenableFuture<ParallelAnalysisResult>> futures) {
    final FutureCallback<ParallelAnalysisResult> callback =
        new FutureCallback<ParallelAnalysisResult>() {
          @Override
          public void onSuccess(ParallelAnalysisResult pResult) {
            if (pResult.hasValidReachedSet()) {
              // cancel other computations
              logger.log(
                  Level.INFO,
                  "One of the parallel analyses has finished, cancelling all other runs.");
              futures.forEach(f -> f.cancel(true));
            }
          }

          @Override
          public void onFailure(Throwable pT) {}
        };
    futures.forEach(f -> Futures.addCallback(f, callback));
  }

  private Callable<ParallelAnalysisResult> createAlgorithm(
      Path singleConfigFileName, int numberOfAnalysis) {
    final Configuration singleConfig;
    final Algorithm algorithm;
    final ReachedSet reached;
    final LogManager singleLogger;
    final ConfigurableProgramAnalysis cpa;
    final boolean supplyReached;
    final boolean supplyRefinableReached;
    final CoreComponentsFactory coreComponents;

    try {
      Iterable<String> parts =
          CONFIG_FILE_CONDITION_SPLITTER.split(singleConfigFileName.toString());
      if (Iterables.size(parts) > 2) {
        throw new InvalidConfigurationException(
            singleConfigFileName.toString()
                + " is not a valid configuration for a parallel analysis.");
      }
      Iterator<String> configIt = parts.iterator();
      singleConfigFileName = Paths.get(configIt.next());
      supplyReached = Iterables.contains(parts, "supply-reached");
      supplyRefinableReached = Iterables.contains(parts, "supply-reached-refinable");

      singleConfig = createSingleConfig(singleConfigFileName);

      ShutdownManager singleShutdownManager =
          ShutdownManager.createWithParent(shutdownManager.getNotifier());

      singleLogger = logger.withComponentName("Parallel analysis " + numberOfAnalysis);
      ResourceLimitChecker singleLimits =
          ResourceLimitChecker.fromConfiguration(singleConfig, singleLogger, singleShutdownManager);
      singleLimits.start();

      coreComponents =
          new CoreComponentsFactory(
              singleConfig,
              singleLogger,
              singleShutdownManager.getNotifier(),
              aggregatedReachedSetManager.asView());
      cpa = coreComponents.createCPA(cfa, SpecAutomatonCompositionType.TARGET_SPEC);

      // TODO global info will not work correctly with parallel analyses
      // as it is a mutable singleton object
      GlobalInfo.getInstance().setUpInfoFromCPA(cpa);

      algorithm = coreComponents.createAlgorithm(cpa, filename, cfa);

      reached = createInitialReachedSet(cpa, mainEntryNode, coreComponents.getReachedSetFactory());

    } catch (IOException | InvalidConfigurationException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Skipping one analysis because the configuration file "
              + singleConfigFileName.toString()
              + " could not be read");
      return () -> {
        return ParallelAnalysisResult.absent();
      };
    } catch (CPAException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Skipping analysis due to problems while creating the necessary components.");
      return () -> {
        return ParallelAnalysisResult.absent();
      };
    }

    return createParallelAnalysis(
        supplyRefinableReached,
        supplyReached,
        algorithm,
        cpa,
        reached,
        singleLogger,
        coreComponents.getReachedSetFactory());
  }

  private Callable<ParallelAnalysisResult> createParallelAnalysis(
      final boolean supplyRefinableReached,
      final boolean supplyReached,
      final Algorithm algorithm,
      final ConfigurableProgramAnalysis cpa,
      final ReachedSet reached,
      final LogManager singleLogger,
      final ReachedSetFactory reachedsetFactory) {
    return () -> {
      try {
        AlgorithmStatus status;
        ReachedSet currentReached = reached;
        ReachedSet oldReached = null;

        if (!supplyRefinableReached) {
          status = algorithm.run(currentReached);
        } else {
          boolean stopAnalysis = false;
          do {
            status = algorithm.run(currentReached);

            for (ReachedSetAdjustingCPA innerCpa :
                CPAs.asIterable(cpa).filter(ReachedSetAdjustingCPA.class)) {
              if (!innerCpa.adjustPrecision()) {
                stopAnalysis = true;
                break;
              }
            }

            if (oldReached != null) {
              aggregatedReachedSetManager.updateReachedSet(oldReached, currentReached);
            } else {
              aggregatedReachedSetManager.addReachedSet(currentReached);
            }
            singleLogger.log(Level.INFO, "Updating reached set provided to other analyses");

            oldReached = currentReached;
            currentReached = createInitialReachedSet(cpa, mainEntryNode, reachedsetFactory);
          } while (!stopAnalysis);
        }

        // if we do not have reliable information about the analysis we just ignore the result
        if (!reached.hasWaitingState()) {
          singleLogger.log(Level.INFO, "Analysis finished successfully");
          if (supplyReached && !supplyRefinableReached && status.isPrecise() && status.isSound()) {
            aggregatedReachedSetManager.addReachedSet(currentReached);
          }
          return ParallelAnalysisResult.of(currentReached, status);
        } else {
          singleLogger.log(
              Level.WARNING,
              "There are still states in the waitlist although the analysis has ended");
        }
      } catch (CPAException e) {
        singleLogger.logUserException(Level.WARNING, e, "Analysis did not finish properly");
      } catch (InterruptedException e) {
        singleLogger.log(Level.INFO, "Analysis was terminated");
      }
      return ParallelAnalysisResult.absent();
    };
  }

  private Configuration createSingleConfig(Path singleConfigFileName)
      throws IOException, InvalidConfigurationException {
    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    singleConfigBuilder.clearOption("parallelAlgorithm.configFiles");
    singleConfigBuilder.clearOption("analysis.useParallelAnalyses");
    singleConfigBuilder.loadFromFile(singleConfigFileName);
    if (globalConfig.hasProperty("specification")) {
      singleConfigBuilder.copyOptionFrom(globalConfig, "specification");
    }

    Configuration singleConfig = singleConfigBuilder.build();
    return singleConfig;
  }

  private ReachedSet createInitialReachedSet(
      ConfigurableProgramAnalysis cpa, CFANode mainFunction, ReachedSetFactory pReachedSetFactory) {
    AbstractState initialState = cpa.getInitialState(mainFunction, getDefaultPartition());
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction, getDefaultPartition());

    ReachedSet reached = pReachedSetFactory.create();
    reached.add(initialState, initialPrecision);
    return reached;
  }

  public class CompoundException extends CPAException {

    private static final long serialVersionUID = -8880889342586540115L;

    private final List<CPAException> exceptions;

    public CompoundException(String pMsg, List<CPAException> pExceptions) {
      super(pMsg);
      exceptions = Collections.unmodifiableList(new ArrayList<>(pExceptions));
    }

    public List<CPAException> getExceptions() {
      return exceptions;
    }
  }
}

class ParallelAnalysisResult {

  private final ReachedSet reached;
  private final AlgorithmStatus status;

  private ParallelAnalysisResult(ReachedSet pReached, AlgorithmStatus pStatus) {
    reached = pReached;
    status = pStatus;
  }

  public static ParallelAnalysisResult of(ReachedSet pReached, AlgorithmStatus pStatus) {
    return new ParallelAnalysisResult(pReached, pStatus);
  }

  public static ParallelAnalysisResult absent() {
    return new ParallelAnalysisResult(null, null);
  }

  public boolean hasValidReachedSet() {
    if (reached == null || status == null) {
      return false;
    }

    return (from(reached).anyMatch(IS_TARGET_STATE) && status.isPrecise())
        || status.isSound() && !reached.hasWaitingState();
  }

  public ReachedSet getReached() {
    return reached;
  }

  public AlgorithmStatus getStatus() {
    return status;
  }
}
