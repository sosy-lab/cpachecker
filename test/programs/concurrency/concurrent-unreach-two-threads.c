// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Concurrent violation that only needs the *second* of two created threads: `writer`
// writes x=1 before main reads x, so 'if (x == 1)' is taken and reach_error() is called.
// The first thread is irrelevant for the violation, so a witness does not have to
// mention its creation.
// Property: G ! call(reach_error())

#include <pthread.h>

void reach_error() {}

int x = 0;
int y = 0;

void *idle(void *arg) {
  y = 1;
  return NULL;
}

void *writer(void *arg) {
  x = 1;
  return NULL;
}

int main(void) {
  pthread_t t1, t2;
  pthread_create(&t1, NULL, idle, NULL);
  pthread_create(&t2, NULL, writer, NULL);
  if (x == 1) reach_error();
  pthread_join(t1, NULL);
  pthread_join(t2, NULL);
  return 0;
}
