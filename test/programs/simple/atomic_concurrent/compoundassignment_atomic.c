// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

_Atomic int counter = 0;

void *thread(void *arg) {
  counter += 1;
  return NULL;
}

int main() {
  pthread_t t1, t2;

  pthread_create(&t1, NULL, thread, NULL);
  pthread_create(&t2, NULL, thread, NULL);
  pthread_join(t1, NULL);
  pthread_join(t2, NULL);
  if (counter != 2) {
    // this point is only reachable if the increment is not interpreted as atomic
    ERROR: {reach_error();abort();}
  }

  return 0;
}
