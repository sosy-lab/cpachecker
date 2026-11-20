// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The mutex bounds x by 10, so the product does not overflow.

#include <pthread.h>

int x = 0;
pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;

void *t(void *arg) {
  pthread_mutex_lock(&m);
  if (x < 10) {
    x = x + 1;
  }
  pthread_mutex_unlock(&m);
  return 0;
}

int main() {
  pthread_t a, b;
  pthread_create(&a, 0, t, 0);
  pthread_create(&b, 0, t, 0);
  pthread_join(a, 0);
  pthread_join(b, 0);
  x = x * 200000000;
  return 0;
}
