// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The data race is between the write of the thread and a read of main in the
// controlling expression of a loop with an empty body.
// Property: G ! data-race

// Avoid using pre-processor here
typedef unsigned long int pthread_t;

union pthread_attr_t
{
  char __size[56];
  long int __align;
};

typedef union pthread_attr_t pthread_attr_t;

extern int pthread_create (pthread_t *__restrict __newthread,
      const pthread_attr_t *__restrict __attr,
      void *(*__start_routine) (void *),
      void *__restrict __arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));

extern int pthread_join (pthread_t __th, void **__thread_return);

int flag = 0;

void *worker(void *arg) {
  flag = 1;
  return 0;
}

int main(void) {
  pthread_t id;
  pthread_create(&id, 0, worker, 0);
  while (flag == 0);
  pthread_join(id, 0);
  return 0;
}
