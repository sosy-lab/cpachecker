// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *malloc(unsigned long size);
extern void free(void *ptr);

// Dereferences a null pointer if the condition does not hold.
void check(int condition) {
  if (!condition) {
    int *null = 0;
    *null = 0;
  }
}

// Machine model: LP64. Expected: true for valid-memsafety.
// &*p is p, and &h[i] is h + i for memory on the heap (C11 6.5.3.2 (3)).
int main(void) {
  int a[3] = { 0, 1, 2 };
  int *p = a;
  check(&*p == p);
  check(&*(p + 1) == &a[1]);

  int *h = malloc(4 * sizeof(int));
  if (h == 0) {
    return 0;
  }

  int *second = &h[1];
  check(second == h + 1);
  *second = 7;
  check(h[1] == 7);

  int *third = &*(h + 2);
  check(third == &h[2]);
  *third = 8;
  check(*(h + 2) == 8);

  // The address after the last element may be computed, but not dereferenced.
  check(&h[4] - second == 3);

  free(h);
  return 0;
}
