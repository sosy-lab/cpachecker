// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Same shape as overflow_unsafe.c but no thread's arithmetic can overflow.
#include <pthread.h>

int x = 0;

void *incrementer(void *arg) {
  x = x + 1;
  return 0;
}

void *other(void *arg) {
  int y = 0;
  y++;
  return 0;
}

int main() {
  pthread_t t1, t2;
  pthread_create(&t1, 0, incrementer, 0);
  pthread_create(&t2, 0, other, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  return 0;
}
