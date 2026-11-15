// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint(void);

void swap(int *a, int *b) {
  int tmp = *a;
  *a = *b;
  *b = tmp;
}

// Machine model: LP64. Expected: true for valid-memsafety.
// The index k is unknown, but always within the bounds of the array.
int main(void) {
  int array[3] = { 0, 1, 2 };
  unsigned int k = __VERIFIER_nondet_uint() % 3U;

  swap(&array[0], &array[k]);

  return 0;
}
