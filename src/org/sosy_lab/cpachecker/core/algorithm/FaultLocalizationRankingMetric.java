// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.dstar.DStarSuspiciousBuilder;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.ochiai.OchiaiSuspiciousBuilder;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.tarantula.TarantulaSuspiciousBuilder;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation.CoverageInformation;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation.FaultLocalizationFault;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation.SafeCase;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "FaultLocalization")
public class FaultLocalizationRankingMetric implements Algorithm, StatisticsProvider, Statistics {
  @Option(
      secure = true,
      name = "type",
      toUppercase = true,
      values = {"TARANTULA", "OCHIAI", "DSTAR"},
      description = "please select a ranking algorithm")
  private String rankingAlgorithmType = "TARANTULA";

  private final StatTimer totalTime = new StatTimer("Total time");
  private final Algorithm algorithm;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public FaultLocalizationRankingMetric(
      Algorithm pAlgorithm,
      ShutdownNotifier pShutdownNotifier,
      final LogManager pLogger,
      Configuration config) {
    algorithm = pAlgorithm;
    this.shutdownNotifier = pShutdownNotifier;
    this.logger = pLogger;
    try {
      config.inject(this);
    } catch (InvalidConfigurationException e) {
      pLogger.log(
          Level.INFO,
          "Invalid configuration given to "
              + getClass().getSimpleName()
              + ". Using defaults instead.");
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    totalTime.start();
    AlgorithmStatus status = algorithm.run(reachedSet);
    FluentIterable<CounterexampleInfo> counterExamples = getCounterexampleInfos(reachedSet);

    SafeCase safeCase = new SafeCase(reachedSet);
    FailedCase failedCase = new FailedCase(reachedSet);

    FaultLocalizationInfo info;
    Set<ARGPath> safePaths = safeCase.getSafePaths();
    Set<ARGPath> errorPaths = failedCase.getErrorPaths();

    CoverageInformation coverageInformation = new CoverageInformation(failedCase, shutdownNotifier);
    List<Fault> faults;
    faults = getFaultsByOption(safePaths, errorPaths, coverageInformation);

    logger.log(Level.INFO, faults);
    for (CounterexampleInfo counterexample : counterExamples) {
      info = new FaultLocalizationInfo(faults, counterexample);
      info.getHtmlWriter().hideTypes(InfoType.RANK_INFO);
      info.apply();
    }
    totalTime.stop();
    return status;
  }
  /** Find and return all error labels */
  private FluentIterable<CounterexampleInfo> getCounterexampleInfos(ReachedSet reachedSet) {
    return Optionals.presentInstances(
        from(reachedSet)
            .filter(AbstractStates::isTargetState)
            .filter(ARGState.class)
            .transform(ARGState::getCounterexampleInformation));
  }
  /**
   * Gets list of corresponding faults by option which is set by variable <code>rankingAlgorithmType<code/>
   *
   * @param pSafePaths set of all safe paths
   * @param pErrorPaths set of all error paths
   * @param pCoverageInformation coverage information of each CFAEdge
   * @return list of faults
   */
  private List<Fault> getFaultsByOption(
      Set<ARGPath> pSafePaths, Set<ARGPath> pErrorPaths, CoverageInformation pCoverageInformation)
      throws InterruptedException {
    List<Fault> faults;
    logger.log(Level.INFO, "ranking algorithm with " + rankingAlgorithmType + " starts");

    if (rankingAlgorithmType.equals("TARANTULA")) {
      TarantulaSuspiciousBuilder tarantulaRanking = new TarantulaSuspiciousBuilder();
      faults =
          new FaultLocalizationFault()
              .getFaults(
                  tarantulaRanking.calculateSuspiciousForCFAEdge(
                      pSafePaths, pErrorPaths, pCoverageInformation));
    } else if (rankingAlgorithmType.equals("DSTAR")) {
      DStarSuspiciousBuilder dStarRanking = new DStarSuspiciousBuilder();
      faults =
          new FaultLocalizationFault()
              .getFaults(
                  dStarRanking.calculateSuspiciousForCFAEdge(
                      pSafePaths, pErrorPaths, pCoverageInformation));
    } else {
      OchiaiSuspiciousBuilder ochiaiRanking = new OchiaiSuspiciousBuilder();
      faults =
          new FaultLocalizationFault()
              .getFaults(
                  ochiaiRanking.calculateSuspiciousForCFAEdge(
                      pSafePaths, pErrorPaths, pCoverageInformation));
    }
    return faults;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    if (algorithm instanceof Statistics) {
      statsCollection.add((Statistics) algorithm);
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter w0 = StatisticsWriter.writingStatisticsTo(out);
    w0.put("Total time with " + rankingAlgorithmType + "  ", totalTime);
  }

  @Override
  public @Nullable String getName() {
    return "Fault Localization with " + rankingAlgorithmType;
  }
}
