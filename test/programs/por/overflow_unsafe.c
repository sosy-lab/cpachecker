// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A thread overflows a shared signed int; a second thread just runs
// concurrently so this exercises the overflow property in a threaded setting.
#include <pthread.h>
#include <limits.h>

int x = INT_MAX;

void *overflower(void *arg) {
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
  pthread_create(&t1, 0, overflower, 0);
  pthread_create(&t2, 0, other, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  return 0;
}
