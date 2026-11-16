// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The second increment overflows only if the setter runs before the first one.

#include <pthread.h>
#include <limits.h>

int x = 0;

void *setter(void *arg) {
  x = INT_MAX - 1;
  return 0;
}

void *incr(void *arg) {
  x = x + 1;
  x = x + 1;
  return 0;
}

int main() {
  pthread_t a, b;
  pthread_create(&a, 0, setter, 0);
  pthread_create(&b, 0, incr, 0);
  return 0;
}
