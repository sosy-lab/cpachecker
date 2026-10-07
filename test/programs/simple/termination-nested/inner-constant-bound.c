// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

// Each iteration of the outer loop contains iterations of the inner loop
int main() {
  int n = __VERIFIER_nondet_int();
  int i = 0;
  while (i < n) {
    int j = 0;
    while (j < 3) {
      j++;
    }
    i++;
  }
  return 0;
}
