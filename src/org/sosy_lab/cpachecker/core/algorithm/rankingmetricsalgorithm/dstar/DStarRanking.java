// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.dstar;

import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.Ranking;

public class DStarRanking extends Ranking {
  /**
   * Calculates suspicious of DStar algorithm.
   *
   * @param pFailed Is the number of pFailed cases in each edge.
   * @param pPassed Is the number of pPassed cases in each edge.
   * @param totalFailed Is the total number of all possible error paths.
   * @return Calculated suspicious.
   */
  @Override
  public double computeSuspicious(
      double pFailed, double pPassed, double totalFailed, double totalPassed) {
    double numerator = Math.pow(pFailed, 2);
    double denominator = (pPassed + (totalFailed - pFailed));
    if (denominator == 0.0) {
      return 0.0;
    }
    return numerator / denominator;
  }
}
