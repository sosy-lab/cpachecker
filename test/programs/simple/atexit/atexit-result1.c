// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern int atexit(void (*function)(void));
extern void exit(int status);

extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { __assert_fail("0", "transformation1.c", 24, "reach_error"); }
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
  ERROR: {reach_error(); abort();}
  }
  return;
}

void f() {
  __VERIFIER_assert(0);
}

int main() {
  int r = atexit(&f);
  if (r) {
    abort();
  }
  return 0;
}
