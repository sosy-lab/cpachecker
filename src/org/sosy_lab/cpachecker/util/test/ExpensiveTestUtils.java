// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

public class ExpensiveTestUtils {

  /**
   * True if running more exhaustive tests is allowed.
   *
   * <p>Use <code>ant tests -DenableExpensiveTests=true</code> to set this flag. The test suite will
   * then generate a much more exhaustive set of input values for the tested methods using this.
   */
  private static final boolean enableExpensiveTests =
      Boolean.parseBoolean(System.getProperty("enableExpensiveTests"));

  /**
   * Returns true if running more exhaustive tests is allowed. Should be used to reduce the
   * test-load for expensive tests that do not need to run all the time.
   *
   * <p>Use <code>ant tests -DenableExpensiveTests=true</code> to set this flag.
   */
  public static boolean isRunExpensiveTests() {
    return enableExpensiveTests;
  }
}
