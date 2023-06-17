// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "parallelAlgorithm")
public class ParallelAlgorithm extends AbstractParallelAlgorithm implements StatisticsProvider {

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
              + " to make this work properly.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<AnnotatedValue<Path>> configFiles;

  private static final String SUCCESS_MESSAGE =
      "One of the parallel analyses has finished successfully, cancelling all other runs.";

  private final Configuration globalConfig;
  private final Specification specification;
  private ParallelAnalysisResult finalResult = null;

  public ParallelAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(
        pLogger,
        pShutdownNotifier,
        pCfa,
        pAggregatedReachedSets,
        new ParallelAlgorithmStatistics(pLogger));
    config.inject(this);

    globalConfig = config;
    specification = checkNotNull(pSpecification);

    ImmutableList.Builder<Callable<ParallelAnalysisResult>> analysesBuilder =
        ImmutableList.builder();

    int i = 0;
    for (AnnotatedValue<Path> p : configFiles) {
      analysesBuilder.add(createParallelAnalysis(p, ++i));
    }
    setAnalyses(analysesBuilder.build());
  }

  @Override
  protected AlgorithmStatus determineAlgorithmStatus(
      ReachedSet pReachedSet, List<ListenableFuture<ParallelAnalysisResult>> pFutures) {
    ForwardingReachedSet forwardingReachedSet = (ForwardingReachedSet) pReachedSet;

    if (finalResult != null) {
      forwardingReachedSet.setDelegate(finalResult.getReached());
      return finalResult.getStatus();
    }

    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  @Override
  protected void handleSingleFutureResult(
      ListenableFuture<ParallelAnalysisResult> singleFuture,
      List<ListenableFuture<ParallelAnalysisResult>> futures)
      throws InterruptedException, Error, ExecutionException {

    ParallelAnalysisResult result = singleFuture.get();
    if (result.hasValidReachedSet() && finalResult == null) {
      finalResult = result;
      hasValidResult = true;
      ((ParallelAlgorithmStatistics) stats).successfulAnalysisName = result.getAnalysisName();

      // cancel other computations
      futures.forEach(future -> future.cancel(true));
      logger.log(Level.INFO, result.getAnalysisName() + " finished successfully.");
      shutdownManager.requestShutdown(SUCCESS_MESSAGE);
    } else if (!result.hasValidReachedSet()) {
      logger.log(Level.INFO, result.getAnalysisName() + " finished without usable result.");
    }
  }

  private Callable<ParallelAnalysisResult> createParallelAnalysis(
      final AnnotatedValue<Path> pSingleConfigFileName, final int analysisNumber)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    final Path singleConfigFileName = pSingleConfigFileName.value();
    final boolean supplyReached;
    final boolean supplyRefinableReached;

    final Configuration singleConfig = createSingleConfig(singleConfigFileName, logger);
    if (singleConfig == null) {
      return () -> ParallelAnalysisResult.absent(singleConfigFileName.toString());
    }

    if (pSingleConfigFileName.annotation().isPresent()) {
      supplyRefinableReached =
          switch (pSingleConfigFileName.annotation().orElseThrow()) {
            case "supply-reached" -> {
              supplyReached = true;
              yield false;
            }
            case "supply-reached-refinable" -> {
              supplyReached = false;
              yield true;
            }
            default -> throw new InvalidConfigurationException(
                String.format(
                    "Annotation %s is not valid for config %s in option"
                        + " parallelAlgorithm.configFiles",
                    pSingleConfigFileName.annotation(), pSingleConfigFileName.value()));
          };
    } else {
      supplyReached = false;
      supplyRefinableReached = false;
    }

    return createParallelAnalysis(
        pSingleConfigFileName.toString(),
        analysisNumber,
        singleConfig,
        specification,
        supplyReached,
        supplyRefinableReached);
  }

  @Nullable
  private Configuration createSingleConfig(Path singleConfigFileName, LogManager pLogger) {
    try {
      ConfigurationBuilder singleConfigBuilder = Configuration.builder();
      singleConfigBuilder.copyFrom(globalConfig);
      singleConfigBuilder.clearOption("parallelAlgorithm.configFiles");
      singleConfigBuilder.clearOption("analysis.useParallelAnalyses");
      singleConfigBuilder.loadFromFile(singleConfigFileName);

      Configuration singleConfig = singleConfigBuilder.build();
      NestingAlgorithm.checkConfigs(globalConfig, singleConfig, singleConfigFileName, logger);
      return singleConfig;

    } catch (IOException | InvalidConfigurationException e) {
      pLogger.logUserException(
          Level.WARNING,
          e,
          "Skipping one analysis because the configuration file "
              + singleConfigFileName
              + " could not be read");
      return null;
    }
  }

  private static class ParallelAlgorithmStatistics extends AbstractParallelAlgorithmStatistics {
    private String successfulAnalysisName = null;

    ParallelAlgorithmStatistics(LogManager pLogger) {
      super(pLogger);
    }

    @Override
    public String getName() {
      return "Parallel Algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Number of algorithms used:        " + getNoOfAlgorithmsUsed());
      if (successfulAnalysisName != null) {
        out.println("Successful analysis: " + successfulAnalysisName);
      }
      printSubStatistics(out, result);
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsEntry successfullAnalysisStats = null;
      for (StatisticsEntry subStats : getAllAnalysesStats()) {
        if (isSuccessfulAnalysis(subStats)) {
          successfullAnalysisStats = subStats;
        } else {
          writeSubOutputFiles(pResult, subStats);
        }
      }
      if (successfullAnalysisStats != null) {
        writeSubOutputFiles(pResult, successfullAnalysisStats);
      }
    }

    private boolean isSuccessfulAnalysis(StatisticsEntry pStatEntry) {
      return successfulAnalysisName != null && successfulAnalysisName.equals(pStatEntry.name);
    }

    @Override
    protected Result determineAnalysisResult(Result pResult, String pActualAnalysisName) {
      if (successfulAnalysisName != null && !successfulAnalysisName.equals(pActualAnalysisName)) {
        if (pResult == Result.TRUE) {
          // we need this to let the invariant analysis write a correctness witness if we use
          // k-induction. TODO: find a better fix for this mess.
          return Result.UNKNOWN;
        } else {
          // Signal that this analysis is not important for the final verdict:
          // (especially, do NOT generate a correctness witness)
          return Result.DONE;
        }
      }
      return pResult;
    }
  }

  public interface ReachedSetUpdateListener {

    void updated(ReachedSet pReachedSet);
  }

  public interface ReachedSetUpdater {

    void register(ReachedSetUpdateListener pReachedSetUpdateListener);

    void unregister(ReachedSetUpdateListener pReachedSetUpdateListener);
  }

  public interface ConditionAdjustmentEventSubscriber {

    void adjustmentSuccessful(ConfigurableProgramAnalysis pCpa);

    void adjustmentRefused(ConfigurableProgramAnalysis pCpa);
  }
}
