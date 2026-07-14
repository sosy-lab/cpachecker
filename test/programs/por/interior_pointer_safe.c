// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A thread writes a struct field through an INTERIOR pointer (&g.b, an address in the middle of
// the object, not the object's own base) that main stored in a shared pointer. After the join main
// reads g.b directly. For the analysis to be correct, the write through the interior pointer
// (address base_g + offsetof(b)) must alias main's direct access to g.b (base_g, offset
// offsetof(b)) — i.e. a full byte address base + offset, which the flat memory layout provides.
// SAFE: g.b == 1 after the join.

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
  shared_p = &g.b; // interior pointer: base_g + offsetof(b)
  pthread_t t;
  pthread_create(&t, 0, writer, 0);
  pthread_join(t, 0);
  if (g.b != 1) {
    ERROR: {reach_error(); return 1;}
  }
  return 0;
}
