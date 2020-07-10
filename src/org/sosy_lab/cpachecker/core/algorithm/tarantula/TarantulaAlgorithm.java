// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.tarantula;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class TarantulaAlgorithm implements Algorithm, StatisticsProvider, Statistics {
  private final Algorithm algorithm;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  StatTimer totalAnalysisTime = new StatTimer("Time for fault localization");

  public TarantulaAlgorithm(
      Algorithm analysisAlgorithm, ShutdownNotifier pShutdownNotifier, final LogManager pLogger) {
    algorithm = analysisAlgorithm;
    this.shutdownNotifier = pShutdownNotifier;
    this.logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    totalAnalysisTime.start();
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));
    try {

      AlgorithmStatus result = algorithm.run(reachedSet);
      SafeCase safeCase = new SafeCase(reachedSet);
      FailedCase failedCase = new FailedCase(reachedSet);
      // Checks if there is any error paths before starting the algorithm
      if (failedCase.existsErrorPath()) {
        // Checks if there is any safe paths before starting the algorithm
        if (!safeCase.existsSafePath()) {

          logger.log(
              Level.WARNING, "There is no safe Path, the algorithm is therefore not efficient");
        }
        logger.log(Level.INFO, "Start tarantula algorithm ... ");

        runTarantulaProcess(counterExamples, safeCase, failedCase);

      } else {
        logger.log(Level.INFO, "There is no counterexample. No bugs found.");
      }
      return result;
    } finally {
      totalAnalysisTime.stop();
    }
  }

  /**
   * Prints result after calculating suspicious and make the ranking for all edges and then store
   * the results <code>CPALog.txt</code> and make the graphical representations possible
   */
  public void runTarantulaProcess(
      FluentIterable<CounterexampleInfo> pCounterexampleInfo,
      SafeCase safeCase,
      FailedCase failedCase)
      throws InterruptedException {
    FaultLocalizationInfo info;
    TarantulaRanking ranking = new TarantulaRanking(safeCase, failedCase, shutdownNotifier);
    List<Fault> faults = new TarantulaFault().getTarantulaFaults(ranking.getRanked());
    logger.log(Level.INFO, faults);
    for (CounterexampleInfo counterexample : pCounterexampleInfo) {
      info = new FaultLocalizationInfo(faults, counterexample);
      info.getHtmlWriter().hideTypes(InfoType.RANK_INFO);
      info.apply();
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
    w0.put("Tarantula total time", totalAnalysisTime);
  }

  @Override
  public @Nullable String getName() {
    return "Fault Localization With Tarantula";
  }
}
