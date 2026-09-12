// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Expected to yield UNKNOWN: MathSAT5's floating-point theory does not support interpolation.

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
  double sum = 0.0;

  while (sum < 1000000000.0) {
    double v = __VERIFIER_nondet_double();
    __VERIFIER_assume(v >= 0.0);
    sum += sqrt(v);
  }

  __VERIFIER_assert(sum >= 0.0);

  return 0;
}
