// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "overflow_1-2.c", 3, "reach_error"); }

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

// Was modified by me to test Loop Summary, do not Push under any circumstance

int main(void) {
  unsigned int x = 0;
  unsigned int y = 0;
  unsigned int z = 0;

  while (x < 1000) {
    x += 5;
    y = 1 + y;
    z = z;
  }

  __VERIFIER_assert(x % 2 == 0);
}
