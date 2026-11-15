// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint(void);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
// Like address_of/unknown-index-write-unsafe-64.c, but writes array[j] directly instead
// of through a pointer to it.
int main(void) {
  int array[3] = { 0, 1, 2 };
  int j = (int)(__VERIFIER_nondet_uint() % 4U);

  // Invalid: for j == 3, the index is after the last element.
  array[j] = 3;

  return array[0];
}
