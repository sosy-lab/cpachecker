// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

// The progress after the inner loop is undone before it, so the iteration of the outer loop
// must include the part before the inner loop
int main() {
  int i = __VERIFIER_nondet_int();
  int j;
  while (i > 0) {
    i = i - 1;
    j = __VERIFIER_nondet_int();
    while (j < 0) {
      j++;
    }
    i = i + 1;
  }
  return 0;
}
