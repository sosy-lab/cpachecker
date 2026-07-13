// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Reachability check on an _Atomic-qualified, non-integer (float) shared
// variable.
//
// Unlike OC, POR has no type-level notion of atomicity at all: atomicity in POR
// comes only from __VERIFIER_atomic_* / atomic-block function calls
// (MutexFunctions), so an _Atomic variable is just an ordinary shared variable
// whose accesses stay dependent. That is sound (it only forgoes reduction), and
// this test pins it down together with the fact that a float shared variable
// goes through the wrapped analyses unharmed.
//
// One writer, joined before the check, so the value is deterministic: safe.

#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

_Atomic float shared;

void *writer(void *arg) {
  shared = 1.0f;
  return 0;
}

int main() {
  pthread_t t;
  shared = 0.0f;
  pthread_create(&t, 0, writer, 0);
  pthread_join(t, 0);
  if (shared != 1.0f) {
    reach_error();
    abort();
  }
  return 0;
}
