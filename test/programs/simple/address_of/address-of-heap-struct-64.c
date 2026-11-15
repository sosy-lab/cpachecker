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

struct inner {
  int a;
  int b;
};

struct outer {
  int n;
  struct inner in;
  int arr[4];
};

// Machine model: LP64. Expected: true for valid-memsafety.
// &p->f is &(*p).f, i.e. p plus the offset of f, for a struct on the heap.
int main(void) {
  struct outer *p = malloc(sizeof(struct outer));
  if (p == 0) {
    return 0;
  }

  int *n = &p->n;
  check(n == (int *)p);
  *n = 1;
  check(p->n == 1);

  int *b = &(*p).in.b;
  check(b == &p->in.b);
  *b = 2;
  check(p->in.b == 2);

  int *element = &p->arr[2];
  check(element == p->arr + 2);
  *element = 3;
  check(p->arr[2] == 3);

  // The struct pointer is the result of a cast.
  void *untyped = p;
  int *viaCast = &((struct outer *)untyped)->arr[2];
  check(viaCast == element);

  free(p);
  return 0;
}
