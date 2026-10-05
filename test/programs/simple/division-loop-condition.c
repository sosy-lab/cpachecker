// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// InvariantsCPA used the loop condition as widening hint, and narrowing x with
// it wrongly made the state bottom.
extern int __VERIFIER_nondet_int(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *);
void reach_error() { __assert_fail("0", "division-loop-condition.c", 3, "reach_error"); }

int main() {
  int x = __VERIFIER_nondet_int();
  if (x < 3 || x > 14) {
    return 0;
  }
  // terminates with x in [101, 102]
  while (100 / x > -20 / x) {
    x = x + 2;
  }
  if (x == 5) {
    reach_error();
  }
  return 0;
}
