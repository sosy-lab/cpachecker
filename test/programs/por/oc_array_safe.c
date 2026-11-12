// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The thread only ever writes a[1]; a[2] is never touched and keeps its
// zero-initialized value. Exercises per-cell zero initialization of an
// aggregate global and separation of literal array offsets.

#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

int a[4];

void *writer(void *arg) {
  a[1] = 5;
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, writer, 0);
  pthread_join(t, 0);
  if (a[2] != 0) { ERROR: { reach_error(); abort(); } }
  return 0;
}
