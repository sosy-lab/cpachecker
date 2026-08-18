// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int,
                           const char *) __attribute__((__nothrow__, __leaf__))
__attribute__((__noreturn__));

void reach_error() {
  __assert_fail("0", "simple_calculations_safe.c", 20, "reach_error");
}

extern int __VERIFIER_nondet_int();

int main() {
  int y = __VERIFIER_nondet_int();
  if (y < 0) {
    y = y * (-1);
  }
  if (y < 0) {
    reach_error();
  }
  return 0;
}
