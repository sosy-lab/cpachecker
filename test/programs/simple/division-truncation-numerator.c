// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *);
void reach_error() { __assert_fail("0", "division-truncation-numerator.c", 3, "reach_error"); }

int main() {
  int x = __VERIFIER_nondet_int();
  if (x < -21 || x > -10) {
    return 0;
  }
  // x in [-15, -12] afterwards
  if (x / 4 != -3) {
    return 0;
  }
  if (x == -15) {
    reach_error();
  }
  return 0;
}
