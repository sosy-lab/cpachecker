// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Exactly one waiter gets PTHREAD_BARRIER_SERIAL_THREAD (-1) from the release.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[36]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
extern int pthread_join(pthread_t __th, void **__thread_return);
typedef union { char __size[20]; long int __align; } pthread_barrier_t;
typedef union { char __size[4]; int __align; } pthread_barrierattr_t;
extern int pthread_barrier_init(pthread_barrier_t *__restrict __barrier,
    const pthread_barrierattr_t *__restrict __attr, unsigned int __count);
extern int pthread_barrier_wait(pthread_barrier_t *__barrier);
extern void reach_error(void);

pthread_barrier_t b;
int r1, r2;

void *w1(void *arg) {
  r1 = pthread_barrier_wait(&b);
  return 0;
}

void *w2(void *arg) {
  r2 = pthread_barrier_wait(&b);
  return 0;
}

int main() {
  pthread_t t1, t2;
  pthread_barrier_init(&b, 0, 2);
  pthread_create(&t1, 0, w1, 0);
  pthread_create(&t2, 0, w2, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  int serial = (r1 == -1) + (r2 == -1);
  if (serial != 1) {
    reach_error();
  }
  return 0;
}
