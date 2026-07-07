// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// At the default initial loop bound (5) the 7-iteration loop below is cut
// feasibly, so the analysis deepens; at bound 10 the loop fits entirely, the
// unwinding assertion proves no feasible execution was cut, and the verdict
// is a sound TRUE.

#include <pthread.h>

extern void abort(void);
extern void reach_error(void);

int x = 0;

void *adder(void *arg) {
  for (int i = 0; i < 7; i++) {
    x = x + 1;
  }
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, adder, 0);
  pthread_join(t, 0);
  if (x > 7) { ERROR: {reach_error();abort();} }
  return 0;
}
