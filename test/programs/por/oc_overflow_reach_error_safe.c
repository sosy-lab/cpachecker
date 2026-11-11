// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A reachable error function is no violation of no-overflow.

#include <pthread.h>
#include <assert.h>

void reach_error() { assert(0); }

int x = 0;

void *t(void *arg) {
  x = 1;
  return 0;
}

int main() {
  pthread_t a;
  pthread_create(&a, 0, t, 0);
  if (x == 1) {
    reach_error();
  }
  return 0;
}
