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

import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class TarantulaRanking {

  /**
   * Calculates how many total failed cases are in ARG.
   *
   * @param pReachedSet Input.
   * @return how many failed cases are found.
   */
  private int totalFailed(ReachedSet pReachedSet) {
    Set<List<CFAEdge>> allPaths = TarantulaUtils.getAllPossiblePaths(pReachedSet);

    int counterResult = 0;
    for (List<CFAEdge> pAllPath : allPaths) {
      if (TarantulaUtils.isFailedPath(pAllPath, pReachedSet)) {
        counterResult++;
      }
    }
    return counterResult;
  }
  /**
   * Calculates how many total passed cases are in ARG.
   *
   * @param pReachedSet Input.
   * @return how many passed cases are found.
   */
  private int totalPassed(ReachedSet pReachedSet) {
    Set<List<CFAEdge>> allPaths = TarantulaUtils.getAllPossiblePaths(pReachedSet);

    return allPaths.size() - totalFailed(pReachedSet);
  }
  /**
   * Calculates computeSuspicious of tarantula algorithm.
   *
   * @param pFailed Is the number of pFailed cases in each edge.
   * @param pPassed Is the number of pPassed cases in each edge.
   * @param pReachedSet Input.
   * @return Calculated suspicious.
   */
  public double computeSuspicious(double pFailed, double pPassed, ReachedSet pReachedSet) {
    double numerator = pFailed / totalFailed(pReachedSet);

    double denominator =
        (pPassed / totalPassed(pReachedSet)) + (pFailed / totalFailed(pReachedSet));
    if (denominator == 0.0) {
      return 0.0;
    }
    return numerator / denominator;
  }
}
