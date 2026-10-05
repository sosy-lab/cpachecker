// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Reduced example of issue #1208
extern unsigned int __VERIFIER_nondet_uint(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *);
void reach_error() { __assert_fail("0", "division-truncation-denominator.c", 3, "reach_error"); }

int main() {
  unsigned int x = __VERIFIER_nondet_uint();
  if (x < 10 || x > 21) {
    return 0;
  }
  // x in [11, 21] afterwards
  if (20 / x >= 2) {
    return 0;
  }
  if (x == 21) {
    reach_error();
  }
  return 0;
}
