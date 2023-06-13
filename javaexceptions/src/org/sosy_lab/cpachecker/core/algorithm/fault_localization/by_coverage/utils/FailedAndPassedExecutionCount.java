// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.utils;

/**
 * Stores the number of failed executions and passed executions.
 *
 * <p>A failed execution is a program execution that eventually reaches a property violation. A
 * passed execution is a program execution that does never reach a property violation.
 *
 * @see CoverageInformationBuilder
 */
public class FailedAndPassedExecutionCount {

  private final int failedCases;
  private final int passedCases;

  public FailedAndPassedExecutionCount(int pFailedCases, int pPassedCases) {

    failedCases = pFailedCases;
    passedCases = pPassedCases;
  }

  public int getFailedCases() {
    return failedCases;
  }

  public int getPassedCases() {
    return passedCases;
  }

  @Override
  public String toString() {
    return "FaultLocalizationCasesStatus{"
        + "failedCases="
        + failedCases
        + ", passedCases="
        + passedCases
        + '}';
  }
}
