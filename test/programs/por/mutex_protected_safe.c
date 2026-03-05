// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads access shared variables x and y. Thread 1 sets x=10 then y=x
// inside a critical section, thread 2 sets x=20 inside a critical section.
// With mutex protection, thread 2 cannot interleave between thread 1's
// x=10 and y=x, so y can never be 20. The error is unreachable.
#include <pthread.h>

int x = 0;
int y = 0;
pthread_mutex_t m;

void *thread1(void *arg) {
  pthread_mutex_lock(&m);
  x = 10;
  y = x;
  pthread_mutex_unlock(&m);
  return 0;
}

void *thread2(void *arg) {
  pthread_mutex_lock(&m);
  x = 20;
  pthread_mutex_unlock(&m);
  return 0;
}

int main() {
  pthread_mutex_init(&m, 0);
  pthread_t t1, t2;
  pthread_create(&t1, 0, thread1, 0);
  pthread_create(&t2, 0, thread2, 0);
  // With mutex, y is always 0 or 10 (never 20)
  if (y == 20) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
