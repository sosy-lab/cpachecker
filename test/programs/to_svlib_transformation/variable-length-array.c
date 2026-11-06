// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Arrays of variable length must not overlap, also beyond the length that the analysis assumes.
extern int __VERIFIER_nondet_int();

int main() {
  int n = __VERIFIER_nondet_int();
  if (n < 21 || n > 30) {
    return 0;
  }
  int a[n];
  int b[n];
  a[0] = 1;
  b[0] = 2;
  a[n - 1] = 3;
  b[n - 1] = 4;
  if (a[0] != 1 || b[0] != 2) {
    ERROR: goto ERROR;
  }
  return 0;
}
