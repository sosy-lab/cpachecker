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
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.SingleFaultOfRankingAlgo;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.SuspiciousnessMeasure;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.dstar.DStarSuspiciousnessMeasure;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.ochiai.OchiaiSuspiciousnessMeasure;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.tarantula.TarantulaSuspiciousnessMeasure;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation.CoverageInformation;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation.FailedCase;
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

@Options(prefix = "faultLocalization")
public class FaultLocalizationWithCoverage implements Algorithm, StatisticsProvider, Statistics {
  private enum AlgorithmType {
    TARANTULA,
    DSTAR,
    OCHIAI;
  }

  @Option(
      secure = true,
      name = "type",
      description = "Ranking algorithm to use for fault localization")
  private AlgorithmType rankingAlgorithmType = AlgorithmType.TARANTULA;

  private final StatTimer totalTime = new StatTimer("Total time of fault localization");
  private final Algorithm algorithm;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public FaultLocalizationWithCoverage(
      Algorithm pAlgorithm,
      ShutdownNotifier pShutdownNotifier,
      final LogManager pLogger,
      Configuration pConfig)
      throws InvalidConfigurationException {

    pConfig.inject(this);
    algorithm = pAlgorithm;
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    totalTime.start();
    AlgorithmStatus status;
    try {
      status = algorithm.run(reachedSet);
      FluentIterable<CounterexampleInfo> counterExamples = getCounterexampleInfos(reachedSet);

      SafeCase safeCase = new SafeCase(reachedSet);
      FailedCase failedCase = new FailedCase(reachedSet);

      FaultLocalizationInfo info;
      Set<ARGPath> safePaths = safeCase.getSafePaths();
      Set<ARGPath> errorPaths = failedCase.getErrorPaths();

      CoverageInformation coverageInformation =
          new CoverageInformation(shutdownNotifier, safePaths, errorPaths);
      SuspiciousnessMeasure chosenRankingAlgorithm = getSuspiciousBuilder(rankingAlgorithmType);

      for (CounterexampleInfo counterexample : counterExamples) {
        List<SingleFaultOfRankingAlgo> faultsOfRankingAlgo = filterByCounterexamples(chosenRankingAlgorithm.getAllFaultsOfRankingAlgo(
            safePaths, errorPaths, coverageInformation),counterexample);
        List<Fault> faults = getFinalResult(faultsOfRankingAlgo);

        info = new FaultLocalizationInfo(faults, counterexample);
        info.getHtmlWriter().hideTypes(InfoType.RANK_INFO);
        info.apply();
      }
    } finally {
      totalTime.stop();
    }
    return status;
  }

  /** Find and return all error labels. */
  private FluentIterable<CounterexampleInfo> getCounterexampleInfos(ReachedSet reachedSet) {
    return Optionals.presentInstances(
        from(reachedSet)
            .filter(AbstractStates::isTargetState)
            .filter(ARGState.class)
            .transform(ARGState::getCounterexampleInformation));
  }

  /**
   * Gets list of corresponding faults by option which is set by variable <code>rankingAlgorithmType<code/>.
   *
   * @return list of faults.
   */
  public List<Fault> getFinalResult(
       List<SingleFaultOfRankingAlgo> faultsOfRankingAlgo) {
    List<Fault> faults = new ArrayList<>();
    for (SingleFaultOfRankingAlgo singleFault : faultsOfRankingAlgo) {

      Fault fault = new Fault(singleFault.getHint());
      fault.setScore(singleFault.getLineScore());
      faults.add(fault);
    }

    return sortingByScoreReversed(faults);
  }

  public List<SingleFaultOfRankingAlgo> filterByCounterexamples(
      List<SingleFaultOfRankingAlgo> pSingleFaultOfRankingAlgos,
      CounterexampleInfo counterexample) {
    ImmutableSet<CFAEdge> fullPath = ImmutableSet.copyOf(counterexample.getTargetPath().getFullPath());
    List<SingleFaultOfRankingAlgo> faultsOnCounterExampleTrace;
    faultsOnCounterExampleTrace =
        pSingleFaultOfRankingAlgos.stream()
            .filter(fault -> fullPath.contains(fault.getHint().correspondingEdge()))
            .collect(Collectors.toList());

    return faultsOnCounterExampleTrace;
  }

  private List<Fault> sortingByScoreReversed(List<Fault> faults) {
    return faults.stream()
        .filter(f -> f.getScore() != 0)
        .sorted(Comparator.comparing((Fault f) -> f.getScore()).reversed())
        .collect(Collectors.toList());
  }

  private SuspiciousnessMeasure getSuspiciousBuilder(AlgorithmType pAlgorithmType) {
    logger.log(Level.INFO, "ranking-algorithm type: " + pAlgorithmType + " starts");
    switch (pAlgorithmType) {
      case TARANTULA:
        return new TarantulaSuspiciousnessMeasure();
      case DSTAR:
        return new DStarSuspiciousnessMeasure();
      case OCHIAI:
        return new OchiaiSuspiciousnessMeasure();
      default:
        throw new AssertionError("Unexpected ranking-algorithm type: " + pAlgorithmType);
    }
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
    w0.put(totalTime);
  }

  @Override
  public String getName() {
    return "Fault Localization with " + rankingAlgorithmType;
  }
}
