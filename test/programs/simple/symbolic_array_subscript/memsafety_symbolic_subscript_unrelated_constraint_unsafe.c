// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The last constraint before the symbolic subscript does not contain the subscript.

extern int __VERIFIER_nondet_int(void);

int x;
int *ptrs[2] = {&x, 0};

int main(void) {
  int i = __VERIFIER_nondet_int();
  if (i < 0 || i >= 2) {
    return 0;
  }
  int k = __VERIFIER_nondet_int();
  if (k != 1) {
    return 0;
  }
  int **pp = &ptrs[i];
  **pp = 1; // UNSAFE for i == 1
  return 0;
}
