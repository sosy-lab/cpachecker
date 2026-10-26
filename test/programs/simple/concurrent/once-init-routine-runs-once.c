// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The init routine runs only at the first call of pthread_once.

typedef int pthread_once_t;
extern int pthread_once(pthread_once_t *__once_control, void (*__init_routine)(void));
extern void reach_error(void);

int g;
pthread_once_t once = 0;

void init(void) { g++; }

int main() {
  pthread_once(&once, init);
  pthread_once(&once, init);
  if (g != 1) {
    reach_error();
  }
  return 0;
}
