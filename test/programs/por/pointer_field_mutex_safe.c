// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads increment a shared counter, each locking the SAME mutex reached through a pointer
// field, `&(ps->lock)`. The lock argument is not a plain `&globalMutex` a name-based scheme could
// resolve; the two threads are serialized only if the mutex is identified by its flat address (both
// `&(ps->lock)` evaluate to the same address). SAFE: counter == 2 after both joins.

#include <pthread.h>

extern void reach_error(void);

struct S {
  int counter;
  pthread_mutex_t lock;
};

struct S s;
struct S *ps;

void *worker(void *arg) {
  pthread_mutex_lock(&(ps->lock));
  s.counter = s.counter + 1;
  pthread_mutex_unlock(&(ps->lock));
  return 0;
}

int main() {
  s.counter = 0;
  ps = &s;
  pthread_t t1, t2;
  pthread_create(&t1, 0, worker, 0);
  pthread_create(&t2, 0, worker, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  if (s.counter != 2) {
    ERROR: {reach_error(); return 1;}
  }
  return 0;
}
