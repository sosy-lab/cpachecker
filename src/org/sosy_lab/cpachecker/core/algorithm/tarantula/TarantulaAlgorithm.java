/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tarantula;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.SafeCase;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
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
      if (failedCase.existsErrorPath()) {
        if (!safeCase.existsSafePath()) {

          logger.log(
              Level.WARNING, "There is no safe Path, the algorithm is therefore not efficient");
        }
        logger.log(Level.INFO, "Start tarantula algorithm ... ");

        getFaultLocations(System.out, counterExamples, safeCase, failedCase);

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
   * the result into <code>Map</code>.
   */
  public void getFaultLocations(
      PrintStream out,
      FluentIterable<CounterexampleInfo> pCounterexampleInfo,
      SafeCase safeCase,
      FailedCase failedCase)
      throws InterruptedException {
    FaultLocalizationInfo info;
    TarantulaRanking ranking = new TarantulaRanking(safeCase, failedCase, shutdownNotifier);
    out.println(ranking.getTarantulaFaults());
    for (CounterexampleInfo counterexample : pCounterexampleInfo) {
      info = new FaultLocalizationInfo(ranking.getTarantulaFaults(), counterexample);
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
