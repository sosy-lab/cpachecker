// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
void reach_error() {}

void increment(int *a, int n) {
  if (n > 0) {
    ++(*a);
    increment(a, n - 1);
  }
}

int main() {
  int a = 0;
  int n = __VERIFIER_nondet_int();
  increment(&a, n);
  if (a == 3) {
    reach_error();
  }
  return 0;
}
