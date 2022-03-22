// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

import static org.sosy_lab.cpachecker.core.CPAcheckerResult.Result.FALSE;
import static org.sosy_lab.cpachecker.core.CPAcheckerResult.Result.TRUE;

import org.sosy_lab.cpachecker.core.CPAcheckerResult;

public class TestResults {
  private String log;
  private CPAcheckerResult checkerResult;

  public TestResults(String pLog, CPAcheckerResult pCheckerResult) {
    log = pLog;
    checkerResult = pCheckerResult;
  }

  public String getLog() {
    return log;
  }

  public CPAcheckerResult getCheckerResult() {
    return checkerResult;
  }

  public void assertIs(CPAcheckerResult.Result expected) {
    if (checkerResult.getResult() != expected) {
      throw new AssertionError(
          String.format(
              "Not true that verification result is %s, it is %s. Log output was:%n---%n%s%n---",
              expected, checkerResult.getResult(), log.trim()));
    }
  }

  public void assertIsSafe() {
    assertIs(TRUE);
  }

  public void assertIsUnsafe() {
    assertIs(FALSE);
  }

  @Override
  public String toString() {
    return log;
  }
}
