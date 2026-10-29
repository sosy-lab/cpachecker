// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

// The middle loop is left by a break after the second run of the inner loop, so an iteration of
// the outer loop visits the heads in the order middle, inner, middle, inner
int main() {
  int n = __VERIFIER_nondet_int();
  int i = 0;
  while (i < n) {
    int j = 0;
    while (1) {
      int k = 0;
      while (k < 2) {
        k++;
      }
      j++;
      if (j >= 2) {
        break;
      }
    }
    i++;
  }
  return 0;
}
