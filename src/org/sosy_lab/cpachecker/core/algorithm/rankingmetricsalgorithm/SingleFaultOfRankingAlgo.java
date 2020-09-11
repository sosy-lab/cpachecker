// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm;

import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;

/**
 * Class represents single fault of the chosen ranking-algorithm with coverage where each line has a
 * Fault contributions contains CFAEdge, corresponding code line number and the suspicious score.
 */
public class SingleFaultOfRankingAlgo {

  private final double lineScore;
  private final int lineNumber;
  private final FaultContribution hint;

  public SingleFaultOfRankingAlgo(double pLineScore, FaultContribution pHint, int pLineNumber) {
    this.lineScore = pLineScore;
    this.lineNumber = pLineNumber;
    this.hint = pHint;
  }

  public double getLineScore() {
    return lineScore;
  }

  public FaultContribution getHint() {
    return hint;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  @Override
  public String toString() {
    return "SingleFaultOfRankingAlgo{"
        + "lineScore="
        + lineScore
        + ", lineNumber="
        + lineNumber
        + ", hint="
        + hint
        + '}';
  }
}
