// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The sibling of interior_pointer_safe.c: the error is on exactly the value the thread writes
// through the interior pointer, so it IS reachable. This guards the other direction — an interior
// pointer must NOT be treated as pointing at the whole object's base (which would make the write
// land on g.a instead of g.b and hide the bug). UNSAFE: g.b == 1 after the join.

#include <pthread.h>

extern void reach_error(void);

struct S {
  int a;
  int b;
};

struct S g = {0, 0};
int *shared_p;

void *writer(void *arg) {
  *shared_p = 1; // writes g.b through the interior pointer
  return 0;
}

int main() {
  shared_p = &g.b;
  pthread_t t;
  pthread_create(&t, 0, writer, 0);
  pthread_join(t, 0);
  if (g.b == 1) {
    ERROR: {reach_error(); return 1;}
  }
  return 0;
}
