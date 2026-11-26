// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// pthread_exit is noreturn, as in glibc, but it terminates only its thread, so the join is passed.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
extern int pthread_join(pthread_t __th, void **__thread_return);
extern void pthread_exit(void *__retval) __attribute__((__noreturn__));
extern void reach_error(void);

void *worker(void *arg) {
  pthread_exit(0);
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, worker, 0);
  pthread_join(t, 0);
  reach_error();
  return 0;
}
