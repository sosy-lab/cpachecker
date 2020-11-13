// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage;

public class Tarantula extends SuspiciousnessMeasure {
  /**
   * Calculates suspicious of tarantula measure
   * calculateSuspiciousness=(fail(s)/totalFailed)/((fail(s)/totalFailed) + (pass(s)/totalPassed))).
   *
   * @param pFailed Is the number of pFailed cases in each edge.
   * @param pPassed Is the number of pPassed cases in each edge.
   * @param totalFailed Is the total number of all possible error paths.
   * @param totalPassed Is the total number of all possible safe paths.
   * @return Calculated suspicious.
   */
  @Override
  public double calculateSuspiciousness(
      double pFailed, double pPassed, double totalFailed, double totalPassed) {
    double numerator = pFailed / totalFailed;
    double denominator = (pPassed / totalPassed) + (pFailed / totalFailed);
    if (denominator == 0.0) {
      return 0.0;
    }
    return numerator / denominator;
  }
}
