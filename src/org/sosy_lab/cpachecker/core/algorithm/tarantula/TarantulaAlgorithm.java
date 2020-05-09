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

import java.io.PrintStream;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.SafeCase;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class TarantulaAlgorithm implements Algorithm {
  private final Algorithm analysis;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public TarantulaAlgorithm(
      Algorithm analysisAlgorithm, ShutdownNotifier pShutdownNotifier, final LogManager pLogger) {
    analysis = analysisAlgorithm;
    this.shutdownNotifier = pShutdownNotifier;
    this.logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    StatTimer totalAnalysisTime = new StatTimer("Time for fault localization");
    try {

      AlgorithmStatus result = analysis.run(reachedSet);
      SafeCase safeCase = new SafeCase(reachedSet);
      FailedCase failedCase = new FailedCase(reachedSet);
      if (failedCase.existsErrorPath()) {
        if (!safeCase.existsSafePath()) {

          logger.log(
              Level.WARNING, "There is no safe Path, the algorithm is therefore not efficient");
        }
        logger.log(Level.INFO, "Start tarantula algorithm ... ");
        totalAnalysisTime.start();
        getFaultLocations(System.out, safeCase, failedCase);
      } else {
        logger.log(Level.INFO, "There is no counterexample. No bugs found.");
      }
      logger.log(
          Level.INFO,
          "Consumed time for analysis is: ( " + totalAnalysisTime.getConsumedTime() + " )");
      return result;
    } finally {
      totalAnalysisTime.stop();
    }
  }

  /**
   * Just prints result after calculating suspicious and make the ranking for all edges and then
   * store the result into <code>Map</code>.
   */
  public void getFaultLocations(PrintStream out, SafeCase safeCase, FailedCase failedCase)
      throws InterruptedException {

    TarantulaRanking ranking = new TarantulaRanking(safeCase, failedCase, shutdownNotifier);
    ranking.getRanked().forEach((k, v) -> out.println(k + "--->" + v));
  }
}
