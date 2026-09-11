// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int abs(int);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  long long sum = 0;
  int i = -3;

  // i takes the values -3, -2, -1, 0, 1, 2, so
  // abs(-3)+abs(-2)+abs(-1)+abs(0)+abs(1) = 3+2+1+0+1 = 7.
  while (i < 2) {
    sum += abs(i);
    i++;
  }

  __VERIFIER_assert(sum != 7);

  return 0;
}
