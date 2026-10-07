// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

// Like inner-iterations-change-outer, but j = 2 and j = 1 already form a lasso
int main() {
  int i = __VERIFIER_nondet_int();
  while (i > 0) {
    int j = __VERIFIER_nondet_int();
    while (j > 0) {
      j--;
      i = i + 2;
    }
    i = i - 3;
  }
  return 0;
}
