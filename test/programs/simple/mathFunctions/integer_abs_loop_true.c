// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int abs(int);
extern int __VERIFIER_nondet_int(void);
extern int __VERIFIER_assume(int);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  long long x = 0;

  while (x < 1000000000LL) {
    int n = __VERIFIER_nondet_int();
    __VERIFIER_assume(n != (-2147483647 - 1)); // exclude INT_MIN
    x += abs(n);
  }

  __VERIFIER_assert(x >= 0);

  return 0;
}
