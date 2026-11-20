// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The two threads use different once objects, so both run the init routine.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[36]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
typedef int pthread_once_t;
extern int pthread_once(pthread_once_t *__once_control, void (*__init_routine)(void));

int g;
pthread_once_t once1 = 0;
pthread_once_t once2 = 0;

void init(void) { g = 1; }

void *t1(void *arg) {
  pthread_once(&once1, init);
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, t1, 0);
  pthread_once(&once2, init);
  return 0;
}
