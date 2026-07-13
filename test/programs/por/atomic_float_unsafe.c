// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The load-bearing counterpart of atomic_float_safe.c: two threads write an
// _Atomic float, and shared == 1.0f is reachable iff t2 runs before t1.
//
// _Atomic makes each individual write indivisible; it does NOT order the two
// writes with respect to each other, so both interleavings remain possible. A
// reduction that mistook "atomic" for "need not be interleaved" would drop the
// t2-then-t1 schedule and wrongly report this safe. POR has no type-level
// atomicity handling at all, so it keeps the accesses dependent and finds the
// violation — which is exactly what this test locks in.
//
// Expected verdict: FALSE.

#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

_Atomic float shared;

void *t1(void *arg) {
  shared = 1.0f;
  return 0;
}

void *t2(void *arg) {
  shared = 2.0f;
  return 0;
}

int main() {
  pthread_t a, b;
  pthread_create(&a, 0, t1, 0);
  pthread_create(&b, 0, t2, 0);
  pthread_join(a, 0);
  pthread_join(b, 0);
  if (shared == 1.0f) {
    reach_error();
    abort();
  }
  return 0;
}
