// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

/**
 * The behavior of a function of the program that is known to be the same as that of a well-known
 * function, for example because the program contains its own implementation of a library function.
 *
 * <p>Analyses can use this to handle a call to such a function like a call to the well-known
 * function, in addition to analyzing the program's own implementation, which stays part of the CFA.
 */
public enum KnownFunctionBehavior {

  /**
   * Behaves like {@code void *memset(void *s, int c, size_t n)} with guards, for a function with
   * the parameters {@code (s, c, n)} in this order. {@code s}, {@code c}, and {@code n} are the
   * values of the parameters, i.e., after the arguments are converted to the types of the
   * parameters, and the function never changes them.
   *
   * <p>"With guards" means that, besides setting the bytes, the function may only check conditions
   * and stop the program if they do not hold, e.g., {@code assume_abort_if_not(n == 0 || s)},
   * {@code if (s == 0) abort();}, or a failing assertion. These checks read no memory and change
   * nothing. So the function either stops the program in such a check, or it returns normally, and
   * then the following holds:
   *
   * <ul>
   *   <li>Each of the {@code n} bytes starting at {@code s} holds {@code c} converted to {@code
   *       unsigned char}. Like {@code memset}, the function writes out of bounds if fewer than
   *       {@code n} bytes are valid at {@code s}, which is undefined behavior.
   *   <li>It wrote no other memory, and it either returns {@code s} or has no return value.
   *   <li>No arithmetic overflow happened in the function (the type of {@code n} is unsigned).
   * </ul>
   *
   * <p>So the effect of {@code memset} may only be assumed when the function returns normally. A
   * transformation that replaces calls to such a function by calls to {@code memset} would have to
   * keep the guards.
   */
  MEMSET_WITH_GUARDS,
}
