// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.pcc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

@Options
public class ResultCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class ResultCheckStatistics implements Statistics {

    private final LogManager logger;
    private Timer checkTimer = new Timer();
    private Timer analysisTimer = new Timer();
    private @Nullable StatisticsProvider checkingStatsProvider = null;
    private final Collection<Statistics> checkingStats = new ArrayList<>();
    private @Nullable Statistics proofGenStats = null;

    ResultCheckStatistics(LogManager pLogger) {
      logger = checkNotNull(pLogger);
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println("Time for Verification:          " + analysisTimer);
      pOut.println("Time for Result Check:      " + checkTimer);

      if (checkTimer.getNumberOfIntervals() > 0) {
        pOut.println(
            "Speed up checking:        "
                + ((float) analysisTimer.getSumTime().asNanos())
                    / checkTimer.getSumTime().asNanos());
      }

      if (proofGenStats != null) {
        StatisticsUtils.printStatistics(proofGenStats, pOut, logger, pResult, pReached);
      }

      if (checkingStatsProvider != null) {
        if (checkingStats.isEmpty()) {
          checkingStatsProvider.collectStatistics(checkingStats);
        }
        for (Statistics stats : checkingStats) {
          StatisticsUtils.printStatistics(stats, pOut, logger, pResult, pReached);
        }
      }
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      if (proofGenStats != null) {
        StatisticsUtils.writeOutputFiles(proofGenStats, logger, pResult, pReached);
      }
      if (checkingStatsProvider != null) {
        if (checkingStats.isEmpty()) {
          checkingStatsProvider.collectStatistics(checkingStats);
        }
        for (Statistics stats : checkingStats) {
          StatisticsUtils.writeOutputFiles(stats, logger, pResult, pReached);
        }
      }
    }

    @Override
    public String getName() {
      return "ResultCheckAlgorithm";
    }
  }

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final Algorithm analysisAlgorithm;
  private final CFA analyzedProgram;
  private final Specification specification;
  private final ResultCheckStatistics stats;

  @Option(
      secure = true,
      name = "pcc.resultcheck.writeProof",
      description =
          "Enable to write proof and read it again for validation instead of using the in memory"
              + " solution")
  private boolean writeProof = false;

  @Option(
      secure = true,
      name = "pcc.resultcheck.checkerConfig",
      description = "Configuration for proof checking if differs from analysis configuration")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path checkerConfig;

  public ResultCheckAlgorithm(
      Algorithm pAlgorithm,
      CFA pCfa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    analysisAlgorithm = pAlgorithm;
    analyzedProgram = pCfa;
    logger = pLogger;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
    specification = checkNotNull(pSpecification);
    stats = new ResultCheckStatistics(pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status;

    logger.log(Level.INFO, "Start analysis.");

    try {
      stats.analysisTimer.start();
      status = analysisAlgorithm.run(pReachedSet);
    } finally {
      stats.analysisTimer.stop();
      logger.log(Level.INFO, "Analysis stopped.");
    }

    if (status.isSound() && !pReachedSet.hasWaitingState()) {
      logger.log(Level.INFO, "Analysis successful.", "Start checking analysis result");
      try {
        if (writeProof) {
          status = writeProofAndValidateWrittenProof(pReachedSet);
        } else {
          status = resultCheckingWithoutWritingProof(pReachedSet);
        }
      } catch (InvalidConfigurationException e) {
        status = status.withSound(false);
      } catch (InterruptedException e1) {
        logger.log(Level.INFO, "Timed out. Checking incomplete.");
        return status.withSound(false);
      } finally {
        if (stats.checkTimer.isRunning()) {
          stats.checkTimer.stop();
        }
        logger.log(Level.INFO, "Stop checking analysis result.");
      }

      if (status.isSound()) {
        logger.log(Level.INFO, "Analysis result checked successfully.");
        return status;
      } else {
        pReachedSet.add(
            new DummyErrorState(pReachedSet.getFirstState()), SingletonPrecision.getInstance());
      }
      logger.log(Level.INFO, "Analysis result could not be checked.");

    } else {
      logger.log(Level.WARNING, "Analysis incomplete.");
    }

    return status;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (analysisAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) analysisAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  private AlgorithmStatus resultCheckingWithoutWritingProof(final ReachedSet pVerificationResult)
      throws InvalidConfigurationException, InterruptedException, CPAException {
    stats.checkTimer.start();
    ProofCheckAlgorithm checker =
        new ProofCheckAlgorithm(
            config, logger, shutdownNotifier, pVerificationResult, analyzedProgram, specification);
    stats.checkingStatsProvider = checker;
    return checker.run(initializeReachedSetForChecking(config, pVerificationResult.getCPA()));
  }

  private ReachedSet initializeReachedSetForChecking(
      Configuration pConfig, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, IllegalArgumentException, InterruptedException {
    CoreComponentsFactory factory =
        new CoreComponentsFactory(pConfig, logger, shutdownNotifier, AggregatedReachedSets.empty());
    ReachedSet reached = factory.createReachedSet(pCpa);

    reached.add(
        pCpa.getInitialState(
            analyzedProgram.getMainFunction(), StateSpacePartition.getDefaultPartition()),
        pCpa.getInitialPrecision(
            analyzedProgram.getMainFunction(), StateSpacePartition.getDefaultPartition()));

    return reached;
  }

  private AlgorithmStatus writeProofAndValidateWrittenProof(final ReachedSet pVerificationResult)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    logger.log(Level.INFO, "Write Proof");
    ProofGenerator proofGen = new ProofGenerator(config, logger, shutdownNotifier);
    stats.proofGenStats = proofGen.generateProofUnchecked(pVerificationResult);

    Configuration checkConfig = config;
    ConfigurableProgramAnalysis checkerCPA = pVerificationResult.getCPA();
    if (checkerConfig != null) {
      try {
        checkConfig = Configuration.builder().copyFrom(config).loadFromFile(checkerConfig).build();
        ReachedSetFactory factory = new ReachedSetFactory(checkConfig, logger);
        checkerCPA =
            new CPABuilder(checkConfig, logger, shutdownNotifier, factory)
                .buildCPAs(analyzedProgram, specification, AggregatedReachedSets.empty());

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Cannot read proof checking configuration.");
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
    }

    GlobalInfo.getInstance().setUpInfoFromCPA(checkerCPA);

    stats.checkTimer.start();
    ProofCheckAlgorithm checker =
        new ProofCheckAlgorithm(
            checkerCPA, checkConfig, logger, shutdownNotifier, analyzedProgram, specification);
    stats.checkingStatsProvider = checker;
    return checker.run(initializeReachedSetForChecking(checkConfig, checkerCPA));
  }
}
