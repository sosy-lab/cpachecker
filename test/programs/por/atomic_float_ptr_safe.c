// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Data-race check: concurrent writes THROUGH an `_Atomic float *`.
//
// `_Atomic float *p` is a (non-atomic) pointer to an _Atomic float, so the
// object written by `*p` is atomic-qualified and concurrent writes to it are
// not a data race. Both threads only ever write through the pointer, never to
// the pointer itself, so this program is race-free.
//
// This pins down that OC resolves atomicity from the type of the accessed
// lvalue rather than the syntactic name: the access `*p` has the pointee type
// `_Atomic float`, so OrderingConsistencyTransferRelation's
// `access.type.getCanonicalType().isAtomic()` must see through the dereference.
// It also covers a non-integer atomic (float), whose events go through the same
// (base, offset) region machinery as any other type.
//
// Note that a genuinely atomic POINTER (`float *_Atomic p`, or the parenthesized
// specifier `_Atomic(float *) p`) is not expressible here at all: CPAchecker's C
// frontend rejects both before the analysis runs (a CParserException for
// "_Atomic in unsupported locations", and a CDT syntax error, respectively), so
// OC/POR never have to represent one.

#include <pthread.h>

_Atomic float shared;

_Atomic float *p = &shared;

void *writer1(void *arg) {
  *p = 1.0f;
  return 0;
}

void *writer2(void *arg) {
  *p = 2.0f;
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
