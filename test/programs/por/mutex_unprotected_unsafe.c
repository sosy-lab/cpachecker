// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads access shared variables x and y WITHOUT mutex protection.
// Thread 1 sets x=10 then y=x, thread 2 sets x=20.
// Without a mutex, thread 2 can set x=20 between thread 1's x=10 and y=x,
// causing y to become 20. The error IS reachable.
#include <pthread.h>

extern void abort(void);
#include <assert.h>
void reach_error() { assert(0); }

int x = 0;
int y = 0;

void *thread1(void *arg) {
  x = 10;
  y = x;
  return 0;
}

void *thread2(void *arg) {
  x = 20;
  return 0;
}

int main() {
  pthread_t t1, t2;
  pthread_create(&t1, 0, thread1, 0);
  pthread_create(&t2, 0, thread2, 0);
  // Without mutex: interleaving thread1: x=10; thread2: x=20; thread1: y=x => y==20
  if (y == 20) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
