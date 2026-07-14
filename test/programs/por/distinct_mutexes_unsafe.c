// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The counterpart to pointer_field_mutex_safe.c: two threads increment the same counter but each
// locks a DIFFERENT mutex (m1 vs m2). Because the two mutexes have distinct addresses, their
// critical sections do NOT exclude each other, so a lost update is possible and the counter can end
// up 1. UNSAFE. This guards that address-based mutex identity does not over-serialize distinct
// mutexes (which would hide the bug).

#include <pthread.h>

extern void reach_error(void);

int counter = 0;
pthread_mutex_t m1;
pthread_mutex_t m2;

void *w1(void *arg) {
  pthread_mutex_lock(&m1);
  counter = counter + 1;
  pthread_mutex_unlock(&m1);
  return 0;
}

void *w2(void *arg) {
  pthread_mutex_lock(&m2);
  counter = counter + 1;
  pthread_mutex_unlock(&m2);
  return 0;
}

int main() {
  pthread_t t1, t2;
  pthread_create(&t1, 0, w1, 0);
  pthread_create(&t2, 0, w2, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  if (counter == 1) {
    ERROR: {reach_error(); return 1;}
  }
  return 0;
}
