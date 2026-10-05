// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The loop condition is never true. InvariantsCPA used it as widening hint
// because it wrongly considered it implied (x occurs on both sides).
extern int __VERIFIER_nondet_int(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *);
void reach_error() { __assert_fail("0", "loop-condition-same-variable-2.c", 3, "reach_error"); }

int main() {
  int x = __VERIFIER_nondet_int();
  if (x < -24 || x > 0) {
    return 0;
  }
  while (x + 6 == x) {
    x = x + 2;
  }
  if (x == -3) {
    reach_error();
  }
  return 0;
}
