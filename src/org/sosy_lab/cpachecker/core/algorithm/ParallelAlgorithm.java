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

import com.google.common.base.Preconditions;

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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Options(prefix = "parallelAlgorithm")
public class ParallelAlgorithm implements Algorithm {

  @Option(secure = true, required = true, description = "List of files with configurations to use.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> configFiles;

  private final Configuration globalConfig;
  private final LogManager logger;
  private final ShutdownManager shutdownManager;
  private final CFA cfa;
  private final String filename;

  private volatile ReachedSet finalReachedSet = null;
  private CFANode mainEntryNode = null;

  public ParallelAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      String pFilename)
      throws InvalidConfigurationException {
    config.inject(this);

    globalConfig = config;
    logger = checkNotNull(pLogger);
    shutdownManager = ShutdownManager.createWithParent(checkNotNull(pShutdownNotifier));
    cfa = checkNotNull(pCfa);
    filename = checkNotNull(pFilename);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    mainEntryNode = AbstractStates.extractLocation(pReachedSet.getFirstState());
    ForwardingReachedSet forwardingReachedSet = (ForwardingReachedSet) pReachedSet;

    ExecutorService exec = Executors.newFixedThreadPool(configFiles.size());
    int counter = 1;
    for (Path p : configFiles) {
      exec.submit(createAlgorithm(p, counter++));
    }

    exec.shutdown();

    boolean shouldShutdown =
        shutdownManager.getNotifier().shouldShutdown() || finalReachedSet != null;

    // timlimits of analyses are handled via shutdownmanager
    // we check every 10 seconds if the analyses were already shutdown
    // and if not although they should be, shut them down forcefully
    while (!exec.awaitTermination(10, TimeUnit.SECONDS)) {

      // no shutdown although it should have been done
      if (shouldShutdown) {
        exec.shutdownNow();
        logger.log(Level.WARNING, "Forcefully shutdown analyses that didn't do it on their own");
        break;
      }

      shouldShutdown = shutdownManager.getNotifier().shouldShutdown() || finalReachedSet != null;
    }

    if (finalReachedSet != null) {
      forwardingReachedSet.setDelegate(finalReachedSet);
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private synchronized void setFinalReachedSet(ReachedSet set) {
    Preconditions.checkState(finalReachedSet == null);

    //globally terminate all analyses which are not needed anymore
    shutdownManager.requestShutdown("One of the parallel analyses has finished");
    finalReachedSet = set;
  }

  private Runnable createAlgorithm(Path singleConfigFileName, int numberOfAnalysis) {
    final Configuration singleConfig;
    final Algorithm algorithm;
    final ReachedSet reached;
    final LogManager singleLogger;
    try {
      singleConfig = createSingleConfig(singleConfigFileName);
      ShutdownManager singleShutdownManager =
          ShutdownManager.createWithParent(shutdownManager.getNotifier());

      singleLogger = logger.withComponentName("Parallel analysis " + numberOfAnalysis);
      ResourceLimitChecker singleLimits =
          ResourceLimitChecker.fromConfiguration(singleConfig, singleLogger, singleShutdownManager);
      singleLimits.start();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(
              singleConfig, singleLogger, singleShutdownManager.getNotifier());
      ConfigurableProgramAnalysis cpa =
          coreComponents.createCPA(cfa, SpecAutomatonCompositionType.TARGET_SPEC);

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
      return () -> {};
    } catch (CPAException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Skipping analysis due to problems while creating the necessary components.");
      return () -> {};
    }

    return () -> {
      try {
        AlgorithmStatus status = algorithm.run(reached);

        // if we do not have reliable information about the analysis we
        // just ignore the result
        if (status.isPrecise() && status.isSound()) {
          setFinalReachedSet(reached);
          singleLogger.log(Level.INFO, "Analysis finished successfully");
        }
      } catch (CPAException e) {
        singleLogger.logUserException(Level.WARNING, e, "Analysis did not finish properly");
      } catch (InterruptedException e) {
        singleLogger.log(Level.INFO, "Analysis was terminated");
      }
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
    AbstractState initialState =
        cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());

    ReachedSet reached = pReachedSetFactory.create();
    reached.add(initialState, initialPrecision);
    return reached;
  }
}
