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
import com.google.common.collect.ImmutableList;
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
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.DStar;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.Ochiai;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.SuspiciousnessMeasure;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.Tarantula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.utils.CoverageInformationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.utils.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.utils.SafeCase;
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

@Options(prefix = "faultLocalization.by_coverage")
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
  private AlgorithmType rankingMeasure = AlgorithmType.TARANTULA;

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
    AlgorithmStatus status = algorithm.run(reachedSet);
    totalTime.start();
    try {
      List<CounterexampleInfo> counterExamples = getCounterexampleInfos(reachedSet).toList();

      if (counterExamples.isEmpty()) {
        logger.log(
            Level.INFO,
            "No counterexamples found in computed reached set"
                + " - stopping fault localization early."
                + " If CPAchecker found a property violation,"
                + " consider analysis.alwaysStoreCounterexamples=true");
        return status;
      }

      SafeCase safeCase = new SafeCase(reachedSet);
      FailedCase failedCase = new FailedCase(reachedSet);

      FaultLocalizationInfo info;
      Set<ARGPath> safePaths = safeCase.getSafePaths();
      Set<ARGPath> errorPaths = failedCase.getErrorPaths();

      CoverageInformationBuilder coverageInformation =
          new CoverageInformationBuilder(shutdownNotifier, safePaths, errorPaths);
      SuspiciousnessMeasure suspiciousnessMeasure = createSuspiciousnessMeasure(rankingMeasure);
      final List<Fault> faults =
          getFaultsSortedByRank(
              suspiciousnessMeasure.getAllFaults(safePaths, errorPaths, coverageInformation));

      for (CounterexampleInfo counterexample : counterExamples) {
        List<Fault> faultsForCex = getFaultsForCex(faults, counterexample);

        info = new FaultLocalizationInfo(faultsForCex, counterexample);
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
  public List<Fault> getFaultsSortedByRank(List<Fault> pFaults) {
    return sortingByScoreReversed(pFaults);
  }

  public List<Fault> getFaultsForCex(List<Fault> pFaults, CounterexampleInfo counterexample) {
    ImmutableSet<CFAEdge> fullPath =
        ImmutableSet.copyOf(counterexample.getTargetPath().getFullPath());
    return pFaults.stream()
        .filter(f -> f.stream().anyMatch(e -> fullPath.contains(e.correspondingEdge())))
        // cf.
        // https://gitlab.com/sosy-lab/software/cpachecker/-/commit/50e6fd55278032c0eb7365c3923aed3942bbeaf4#note_486588922
        // Better code would be not relying on mutability or better encapsulation in
        // FaultLocalizationInfo.
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<Fault> sortingByScoreReversed(List<Fault> faults) {
    return faults.stream()
        .filter(f -> f.getScore() != 0)
        .sorted(Comparator.comparing((Fault f) -> f.getScore()).reversed())
        .collect(ImmutableList.toImmutableList());
  }

  private SuspiciousnessMeasure createSuspiciousnessMeasure(AlgorithmType pAlgorithmType) {
    logger.log(Level.INFO, "Ranking-algorithm type: " + pAlgorithmType + " starts");
    switch (pAlgorithmType) {
      case TARANTULA:
        return new Tarantula();
      case DSTAR:
        return new DStar();
      case OCHIAI:
        return new Ochiai();
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
    return getClass().getCanonicalName() + "(" + rankingMeasure + ")";
  }
}
