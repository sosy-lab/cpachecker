// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads with local-only operations. No shared variable is modified after
// thread creation, so the program is safe regardless of interleaving.
#include <pthread.h>

int x = 0;

void *thread1(void *arg) {
  int local1 = 10;
  local1 = local1 + 1;
  return 0;
}

void *thread2(void *arg) {
  int local2 = 20;
  local2 = local2 + 2;
  return 0;
}

int main() {
  pthread_t t1, t2;
  pthread_create(&t1, 0, thread1, 0);
  pthread_create(&t2, 0, thread2, 0);
  return 0;
}
