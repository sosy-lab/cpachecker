// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Without other threads, the wait at a barrier for two threads blocks forever.

typedef union { char __size[20]; long int __align; } pthread_barrier_t;
typedef union { char __size[4]; int __align; } pthread_barrierattr_t;
extern int pthread_barrier_init(pthread_barrier_t *__restrict __barrier,
    const pthread_barrierattr_t *__restrict __attr, unsigned int __count);
extern int pthread_barrier_wait(pthread_barrier_t *__barrier);
extern void reach_error(void);

pthread_barrier_t b;

int main() {
  pthread_barrier_init(&b, 0, 2);
  pthread_barrier_wait(&b);
  reach_error();
  return 0;
}
