// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The overflow is in a branch condition.

#include <pthread.h>
#include <limits.h>

int x = INT_MAX;

void *t(void *arg) {
  if (x + 1 > 0) {
    x = 0;
  }
  return 0;
}

int main() {
  pthread_t a;
  pthread_create(&a, 0, t, 0);
  return 0;
}
