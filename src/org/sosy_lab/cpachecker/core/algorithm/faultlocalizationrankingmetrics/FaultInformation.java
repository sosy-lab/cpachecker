// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.faultlocalizationrankingmetrics;

import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;

/** Class represents a special fault where a line has a set of Fault contributions */
public class FaultInformation {

  private final double lineScore;
  private final int lineNumber;
  private final Set<FaultContribution> hints;

  public FaultInformation(double pLineScore, Set<FaultContribution> pHints, int pLineNumber) {
    this.lineScore = pLineScore;
    this.lineNumber = pLineNumber;
    this.hints = pHints;
  }

  public double getLineScore() {
    return lineScore;
  }

  public Set<FaultContribution> getHints() {
    return hints;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  @Override
  public String toString() {
    return "FaultInformation{"
        + "lineScore="
        + lineScore
        + ", lineNumber="
        + lineNumber
        + ", hints="
        + hints
        + '}';
  }
}
