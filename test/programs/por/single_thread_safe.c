// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Single thread, no concurrency. The program is safe.
#include <pthread.h>

int x = 0;

void *worker(void *arg) {
  x = 42;
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, worker, 0);
  return 0;
}
