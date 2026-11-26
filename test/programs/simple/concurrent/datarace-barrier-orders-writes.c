// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The barrier orders the write in t1 before the write in main.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[36]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
typedef union { char __size[20]; long int __align; } pthread_barrier_t;
typedef union { char __size[4]; int __align; } pthread_barrierattr_t;
extern int pthread_barrier_init(pthread_barrier_t *__restrict __barrier,
    const pthread_barrierattr_t *__restrict __attr, unsigned int __count);
extern int pthread_barrier_wait(pthread_barrier_t *__barrier);

int g;
pthread_barrier_t b;

void *t1(void *arg) {
  g = 1;
  pthread_barrier_wait(&b);
  return 0;
}

int main() {
  pthread_t t;
  pthread_barrier_init(&b, 0, 2);
  pthread_create(&t, 0, t1, 0);
  pthread_barrier_wait(&b);
  g = 2;
  return 0;
}
