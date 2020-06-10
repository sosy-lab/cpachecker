// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure;

public class TarantulaCasesStatus {

  private final int failedCases;
  private final int passedCases;

  public TarantulaCasesStatus(int pFailedCases, int pPassedCases) {

    this.failedCases = pFailedCases;
    this.passedCases = pPassedCases;
  }

  public int getFailedCases() {
    return failedCases;
  }

  public int getPassedCases() {
    return passedCases;
  }
}
