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
 * <p>This only describes what the function does when it returns normally. Analyses can use it to
 * handle a call to such a function like a call to the well-known function, in addition to analyzing
 * the program's own implementation, which stays part of the CFA. The function does not read memory,
 * so it does not matter whether an analysis applies the effect of the well-known function before or
 * after the body.
 */
public enum KnownFunctionBehavior {

  /**
   * Behaves like {@code void *memset(void *s, int c, size_t n)} for a function with the parameters
   * {@code (s, c, n)} in this order, where {@code s}, {@code c}, and {@code n} are the values of
   * the parameters, i.e., after the arguments are converted to the types of the parameters: when it
   * returns normally, each of the {@code n} bytes starting at {@code s} holds {@code c} converted
   * to {@code unsigned char}. It reads no memory, writes no other memory, and either returns {@code
   * s} or has no return value.
   *
   * <p>The type of {@code n} is unsigned, and the function itself cannot cause an arithmetic
   * overflow. Like {@code memset}, it writes out of bounds if fewer than {@code n} bytes are valid
   * at {@code s}, which is undefined behavior.
   */
  MEMSET,
}
