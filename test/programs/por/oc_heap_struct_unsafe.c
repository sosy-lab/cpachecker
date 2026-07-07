// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// main heap-allocates a struct S and hands the pointer to the child thread,
// which writes its second field. Exercises multi-cell heap objects and
// field offsets: the write to p->b and the read of p->b in main must be
// related through the field-b region of the same heap allocation, distinct
// from the field-a region.

#include <pthread.h>
#include <stdlib.h>

extern void reach_error(void);
extern void abort(void);

struct S {
  int a;
  int b;
};

struct S *p;

void *writer(void *arg) {
  p->b = 3;
  return 0;
}

int main() {
  p = malloc(sizeof(struct S));
  pthread_t t;
  pthread_create(&t, 0, writer, 0);
  pthread_join(t, 0);
  if (p->b == 3) { ERROR: { reach_error(); abort(); } }
  return 0;
}
