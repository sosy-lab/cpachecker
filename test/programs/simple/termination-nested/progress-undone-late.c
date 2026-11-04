// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

// Like progress-undone-before-inner, but the lasso starts only after 20 iterations
int main() {
  int i = __VERIFIER_nondet_int();
  int c = 20;
  int j;
  while (i > 0) {
    if (c > 0) {
      c--;
    } else {
      i = i - 1;
    }
    j = __VERIFIER_nondet_int();
    while (j < 0) {
      j++;
    }
    if (c == 0) {
      i = i + 1;
    }
  }
  return 0;
}
