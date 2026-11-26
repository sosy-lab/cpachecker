// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Data-race check: the load-bearing counterpart of atomic_float_ptr_safe.c, and
// a trap that a name-based (rather than type-based) atomicity check would fall
// into.
//
// In `_Atomic float *p`, the _Atomic qualifier binds to the POINTEE, not to the
// pointer: p itself is an ordinary, non-atomic `float *`. So while writes through
// `*p` are race-free (see the safe sibling), these concurrent writes to `p`
// ITSELF are a genuine data race. An analysis that concluded "p is declared with
// _Atomic, so accesses to p are atomic" would wrongly report this race-free.
//
// Expected verdict: FALSE (data race on p).

#include <pthread.h>

// _Atomic so that &a and &b have type `_Atomic float *` and the assignments to p
// below are well-formed without a qualifier-discarding conversion.
_Atomic float a;
_Atomic float b;

_Atomic float *p;

void *writer1(void *arg) {
  p = &a;
  return 0;
}

void *writer2(void *arg) {
  p = &b;
  return 0;
}

int main() {
  pthread_t t1, t2;
  pthread_create(&t1, 0, writer1, 0);
  pthread_create(&t2, 0, writer2, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  return 0;
}
