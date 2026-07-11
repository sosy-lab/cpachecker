// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads write to a shared variable, both created and joined through an
// array of handles (pthread_create(&t[i], ...) / pthread_join(t[i], ...)),
// exercising the general candidate-branching join resolution. Under some
// interleaving, an error is reachable: after both threads finish, x could be
// 1 if thread 1 runs before thread 0.

#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

int x = 0;

void *thread0(void *arg) {
  x = 1;
  return 0;
}

void *thread1(void *arg) {
  x = 2;
  return 0;
}

int main() {
  pthread_t t[2];

  pthread_create(&t[0], 0, thread0, 0);
  pthread_create(&t[1], 0, thread1, 0);

  pthread_join(t[0], 0);
  pthread_join(t[1], 0);

  if (x == 1) {
    reach_error();
    abort();
  }
  return 0;
}
