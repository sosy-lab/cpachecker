// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

// The inner loop terminates, but the outer loop does not
int main() {
  int i = __VERIFIER_nondet_int();
  while (i > 0) {
    int j = 0;
    while (j < 3) {
      j++;
    }
  }
  return 0;
}
