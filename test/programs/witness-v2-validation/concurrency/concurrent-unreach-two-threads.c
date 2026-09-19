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

void reach_error() {}

int x = 0;
int y = 0;

void *idle(void *arg) {
  y = 1;
  return 0;
}

void *writer(void *arg) {
  x = 1;
  return 0;
}

int main(void) {
  pthread_t t1, t2;
  pthread_create(&t1, 0, idle, 0);
  pthread_create(&t2, 0, writer, 0);
  if (x == 1) reach_error();
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  return 0;
}
