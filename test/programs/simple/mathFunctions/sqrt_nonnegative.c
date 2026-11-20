// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern double sqrt(double);
extern double __VERIFIER_nondet_double(void);
extern int __VERIFIER_assume(int);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  double x = __VERIFIER_nondet_double();
  __VERIFIER_assume(x >= 0.0);

  double r = sqrt(x);

  // The square root of a nonnegative number is never negative
  __VERIFIER_assert(r >= 0.0);

  return 0;
}
