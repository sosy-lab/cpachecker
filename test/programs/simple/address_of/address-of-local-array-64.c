// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Dereferences a null pointer if the condition does not hold.
void check(int condition) {
  if (!condition) {
    int *null = 0;
    *null = 0;
  }
}

// Machine model: LP64. Expected: true for valid-memsafety.
// &a[i] is a + i for an array on the stack (C11 6.5.3.2 (3)).
int main(void) {
  int a[5] = { 0, 1, 2, 3, 4 };

  int *p = &a[2];
  check(p == a + 2);
  *p = 7;
  check(a[2] == 7);

  check(&a[0] == a);
  // a[2] is 2[a]
  check(&2[a] == p);

  // The address after the last element may be computed, but not dereferenced.
  int *end = &a[5];
  check(end == a + 5);
  check(end - p == 3);

  int matrix[2][3] = { { 0, 0, 0 }, { 0, 0, 0 } };
  int *q = &matrix[1][2];
  *q = 5;
  check(matrix[1][2] == 5);
  check(q == &matrix[0][0] + 5);

  return 0;
}
