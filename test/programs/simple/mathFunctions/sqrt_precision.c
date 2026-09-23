// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern double sqrt(double);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  double x = 9.0;
  double r = sqrt(x);

  // sqrt(9.0) is exactly 3.0
  __VERIFIER_assert(r != 3.0);

  return 0;
}
