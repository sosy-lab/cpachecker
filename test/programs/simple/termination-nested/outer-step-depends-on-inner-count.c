// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

// Does not terminate if every outer iteration chooses j >= 5, which needs at least 5 inner
// iterations
int main() {
  int i = __VERIFIER_nondet_int();
  while (i > 0) {
    int j = __VERIFIER_nondet_int();
    int j0 = j;
    while (j > 0) {
      j--;
    }
    if (j0 >= 5) {
      i++;
    } else {
      i--;
    }
  }
  return 0;
}
