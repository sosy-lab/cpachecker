// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

// From i = 1, j = 11 leads to i = 2 and j = 10 back to i = 1, so the program does not terminate.
// With at most 10 inner iterations, i decreases in every outer iteration, so the outer loop must
// not be proven terminating only for the inner iterations before the inner loop head is covered.
int main() {
  int i = __VERIFIER_nondet_int();
  while (i > 0) {
    int j = __VERIFIER_nondet_int();
    while (j > 0) {
      j--;
      i = i + 2;
    }
    i = i - 21;
  }
  return 0;
}
