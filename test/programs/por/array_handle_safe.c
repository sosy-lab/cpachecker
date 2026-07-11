// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Thread handles stored in an array (pthread_create(&t[i], ...) /
// pthread_join(t[i], ...)): the handle expression is not a plain variable, so
// this exercises the general candidate-branching join resolution, not the
// fast-path hint. Each thread only ever writes its own fixed array slot, so
// the outcome is deterministic regardless of scheduling: this stays safe.

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

  pthread_join(t[0], 0);
  pthread_join(t[1], 0);

  if (result[0] != 1 || result[1] != 2) {
    reach_error();
    abort();
  }
  return 0;
}
