// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern int atexit(void (*function)(void));
extern void exit(int status);

extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { __assert_fail("0", "reach-1.broken.c", 15, "reach_error"); }
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
  ERROR: {reach_error(); abort();}
  }
  return;
}

int c = 0;

void f4() {
  __VERIFIER_assert(++c == 4);
}

void f3() {
  __VERIFIER_assert(++c == 2);
  atexit(f4);
}

void f2() {
  __VERIFIER_assert(++c == 3);
}

void f1() {
  __VERIFIER_assert(++c == 1);
  atexit(f2);
  atexit(f3);
}

/* Test if atexit can be used after exit was called
 * This test was written to clarify a sentence from the C11 standard:
 *   "First, all functions registered by the atexit function are called, in the reverse order of
 *   their registration, *except* that a function is called after any previously registered
 *   functions that had already been called at the time it was registered."
 * Here the exception seems to refer to atexit handlers that have already been exececuted at the time
 * of registration. This can happend when the application is already shutting down and further functions
 * are added to the stack by one of the atexit handlers.
 *
 * In this example we push atexit handlers in this order:
 * f1 -> f2 -> f3 -> f4
 * However, the execution order is:
 * f1 -> f3 -> f4 -> f2
 * and not
 * f4 -> f3 -> f2 -> f1
 * which would be the "reverse order of their registration"
 */
int main() {
  atexit(f1);
  exit();
}
