// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableSet;

/**
 * This class contains methods for checking whether a function is a specific supported stdlib
 * integer function.
 */
public class StdlibFunctions {
  private static final ImmutableSet<String> FUN_ABS = ImmutableSet.of("abs");
  private static final ImmutableSet<String> SQRT = ImmutableSet.of("sqrt");
  private static final ImmutableSet<String> ALL =
      ImmutableSet.<String>builder().addAll(FUN_ABS).addAll(SQRT).build();

  /**
   * Returns whether the given function name is an implemented stdlib function.
   *
   * @param pFunctionName the name of the function to check
   * @return whether the given function name is an implemented stdlib function
   */
  public static boolean isStdlibFunction(String pFunctionName) {
    return ALL.contains(pFunctionName);
  }

  /**
   * Returns whether the given function is the `abs` function.
   *
   * @param pFunctionName the name of the function to check
   * @return whether the given function is the `abs` function
   */
  public static boolean matchesAbs(String pFunctionName) {
    return FUN_ABS.contains(pFunctionName);
  }

  /**
   * Returns whether the given function is the `sqrt` function.
   *
   * @param pFunctionName the name of the function to check
   * @return whether the given function is the `sqrt` function
   */
  public static boolean matchesSqrt(String pFunctionName) {
    return SQRT.contains(pFunctionName);
  }
}
