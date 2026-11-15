// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint(void);
extern void *malloc(unsigned long size);
extern void free(void *ptr);

struct s {
  int n;
  int arr[4];
};

// Machine model: LP64. Expected: true for valid-memsafety.
// Taking the address of an element with an unknown index does not access memory.
int main(void) {
  unsigned int k = __VERIFIER_nondet_uint() % 4U;

  int a[4] = { 0, 1, 2, 3 };
  int *p = &a[k];

  struct s local = { 0, { 0, 0, 0, 0 } };
  int *q = &local.arr[k];

  struct s *heap = malloc(sizeof(struct s));
  if (heap == 0) {
    return 0;
  }
  int *r = &heap->arr[k];
  int *h = &(heap->arr)[k];
  int result = (p != 0) + (q != 0) + (r != 0) + (h != 0);

  free(heap);
  return result;
}
