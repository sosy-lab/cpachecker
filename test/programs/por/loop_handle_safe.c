// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Thread handles joined through an array indexed by a LOOP VARIABLE — the
// common shape in the pthread-race-challenges / libvsync families. The creates
// use literal indices, so each handle gets a fast-path hint keyed on t[0]/t[1]
// (ThreadFunctions#canonicalHandleLvalueKey); the joins use t[i], which that
// key cannot resolve, so they fall back to the general candidate-set branching:
// every live instance is tried, guarded by a synthetic "handle == candidate id"
// assume. That fallback is the path NOT protected by the fast-path hint — the
// one where spurious wrong-match branches can feed handle-identity facts to
// CEGAR interpolation — and no other test in this directory reaches it.
//
// Each worker writes only its own slot, so the result is scheduling-independent
// and the program is safe.

#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

int result[2];

void *worker0(void *arg) {
  result[0] = 1;
  return 0;
}

void *worker1(void *arg) {
  result[1] = 2;
  return 0;
}

int main() {
  pthread_t t[2];

  pthread_create(&t[0], 0, worker0, 0);
  pthread_create(&t[1], 0, worker1, 0);

  for (int i = 0; i < 2; i++) {
    pthread_join(t[i], 0);
  }

  if (result[0] != 1 || result[1] != 2) {
    reach_error();
    abort();
  }
  return 0;
}
