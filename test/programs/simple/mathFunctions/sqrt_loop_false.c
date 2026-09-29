// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Expected to yield UNKNOWN: MathSAT5's floating-point theory does not support interpolation.

extern double sqrt(double);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  double x = 65536.0;
  int i = 0;

  while (i < 3) {
    x = sqrt(x);
    i++;
  }

  __VERIFIER_assert(x != 4.0);

  return 0;
}
