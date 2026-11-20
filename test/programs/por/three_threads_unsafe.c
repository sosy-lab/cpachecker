// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Three threads each write to a shared variable. Under some interleaving the
// final value of x can be any of {1, 2, 3}. The assertion fails when x == 1.
#include <pthread.h>

extern void abort(void);
#include <assert.h>
void reach_error() { assert(0); }

int x = 0;

void *writer1(void *arg) {
  x = 1;
  return 0;
}

void *writer2(void *arg) {
  x = 2;
  return 0;
}

void *writer3(void *arg) {
  x = 3;
  return 0;
}

int main() {
  pthread_t t1, t2, t3;
  pthread_create(&t1, 0, writer1, 0);
  pthread_create(&t2, 0, writer2, 0);
  pthread_create(&t3, 0, writer3, 0);

  if (x == 1) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
