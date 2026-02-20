// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads write to a shared variable. Under some interleaving, an error is
// reachable: after both threads finish, x could be 1 if thread2 runs before
// thread1.
#include <pthread.h>

extern void abort(void);
#include <assert.h>
void reach_error() { assert(0); }

int x = 0;

void *thread1(void *arg) {
  x = 1;
  return 0;
}

void *thread2(void *arg) {
  x = 2;
  return 0;
}

int main() {
  pthread_t t1, t2;
  pthread_create(&t1, 0, thread1, 0);
  pthread_create(&t2, 0, thread2, 0);

  // In at least one interleaving x == 1 (thread2 runs first, then thread1)
  if (x == 1) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
