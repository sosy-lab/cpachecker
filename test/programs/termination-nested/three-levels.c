// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

int main() {
  int n = __VERIFIER_nondet_int();
  int i = 0;
  while (i < n) {
    int j = 0;
    while (j < 3) {
      int k = 0;
      while (k < 2) {
        k++;
      }
      j++;
    }
    i++;
  }
  return 0;
}
