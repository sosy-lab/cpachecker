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
 * handle a call to such a function like a call to the well-known function. The program's own
 * implementation stays part of the CFA.
 */
public enum KnownFunctionBehavior {

  /**
   * Behaves like {@code void *memset(void *s, int c, size_t n)} for a function with the parameters
   * {@code (s, c, n)} in this order: when it returns normally, each of the {@code n} bytes starting
   * at {@code s} holds {@code c} converted to {@code unsigned char}. It writes no other memory of
   * the caller, and it either returns {@code s} or has no return value.
   */
  MEMSET,
}
