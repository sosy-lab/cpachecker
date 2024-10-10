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

void reach_error() { __assert_fail("0", "reach-2.broken.c", 15, "reach_error"); }
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
  ERROR: {reach_error(); abort();}
  }
  return;
}

int counter = 0;

void f0() {
  __VERIFIER_assert(counter == 31);
}

void f1() {
  ++counter;
}

int main() {
  atexit(f0);
  for (int i=0; i<32; i++) {
    atexit(f1);
  }
  return 0;
}
