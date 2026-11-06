// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// After main calls pthread_exit, the other threads continue and can create threads.
// The program has no error, so the analysis must explore all of them.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
extern void pthread_exit(void *__retval);

void *inner(void *arg) {
  return 0;
}

void *outer(void *arg) {
  pthread_t t2;
  pthread_create(&t2, 0, inner, 0);
  return 0;
}

int main() {
  pthread_t t1;
  pthread_create(&t1, 0, outer, 0);
  pthread_exit(0);
}
