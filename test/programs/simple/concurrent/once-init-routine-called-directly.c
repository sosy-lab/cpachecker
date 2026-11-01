// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The program also calls the init routine directly, so g becomes 2.
// The thread that runs the init routine must end at the exit of init,
// although init also returns to call_init.

typedef int pthread_once_t;
extern int pthread_once(pthread_once_t *__once_control, void (*__init_routine)(void));
extern void reach_error(void);

int g;
pthread_once_t once = 0;

void init(void) { g++; }

void call_init(void) { init(); }

int main() {
  call_init();
  pthread_once(&once, init);
  if (g == 2) {
    reach_error();
  }
  return 0;
}
