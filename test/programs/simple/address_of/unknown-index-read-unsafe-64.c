// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint(void);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  int array[3] = { 0, 1, 2 };
  int j = (int)(__VERIFIER_nondet_uint() % 4U);

  // Computing the address after the last element is valid.
  int *p = &array[j];

  // Invalid: for j == 3, p points after the last element.
  return *p;
}
