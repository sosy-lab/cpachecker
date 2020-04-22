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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.SafeCase;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TarantulaAlgorithm implements Algorithm {
  private final Algorithm analysis;
  private final LogManager logger;

  public TarantulaAlgorithm(Algorithm analysisAlgorithm, final LogManager pLogger) {
    analysis = analysisAlgorithm;
    this.logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus result = analysis.run(reachedSet);
    FailedCase errorCase = new FailedCase(reachedSet);
    SafeCase safeCase = new SafeCase(reachedSet);
    if (errorCase.existsErrorPath()) {
      if (!safeCase.existsSafePath()) {

        logger.log(
            Level.WARNING, "There is no safe Path, the algorithm is therefore not efficient");
      }
      logger.log(Level.INFO, "Start tarantula algorithm ... ");
      printResult(System.out, reachedSet);
    } else {
      logger.log(Level.INFO, "No bugs found.");
    }

    return result;
  }

  /**
   * Just prints result after calculating suspicious and make the ranking for all edges and then
   * store the result into <code>Map</code>.
   */
  public void printResult(PrintStream out, ReachedSet reachedSet) {

    TarantulaRanking ranking = new TarantulaRanking(reachedSet);
    ranking.getRanked().forEach((k, v) -> out.println(k + "--->" + v));
  }
}
