// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Same scenario as mutex_protected_safe.c but using C11 threading mutex
// functions (mtx_lock / mtx_unlock) instead of POSIX pthread mutex functions.
// Uses pthread_create for thread creation since POR supports that.
// The error is unreachable thanks to mutex protection.
#include <pthread.h>

// Forward declarations for C11 mutex functions
typedef int mtx_t;
int mtx_init(mtx_t *, int);
int mtx_lock(mtx_t *);
int mtx_unlock(mtx_t *);
void mtx_destroy(mtx_t *);

int x = 0;
int y = 0;
mtx_t m;

void *thread1(void *arg) {
  mtx_lock(&m);
  x = 10;
  y = x;
  mtx_unlock(&m);
  return 0;
}

void *thread2(void *arg) {
  mtx_lock(&m);
  x = 20;
  mtx_unlock(&m);
  return 0;
}

int main() {
  mtx_init(&m, 0);
  pthread_t t1, t2;
  pthread_create(&t1, 0, thread1, 0);
  pthread_create(&t2, 0, thread2, 0);
  // With C11 mutex, y is always 0 or 10 (never 20)
  if (y == 20) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
