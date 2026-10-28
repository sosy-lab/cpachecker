// This file is part of the SV-Witnesses repository as regression test:
// https://gitlab.com/sosy-lab/benchmarking/sv-witnesses
//
// SPDX-FileCopyrightText: 2025 The SV-Witnesses Community
//
// SPDX-License-Identifier: Apache-2.0

// Concurrent data race: two threads write to x with no synchronization.
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

int x = 0;

void* t1_func(void* arg) {
    x = 1;
    return 0;
}

void* t2_func(void* arg) {
    x = 2;
    return 0;
}

int main(void) {
    pthread_t ta, tb;
    pthread_create(&ta, 0, t1_func, 0);
    pthread_create(&tb, 0, t2_func, 0);
    pthread_join(ta, 0);
    pthread_join(tb, 0);
    return 0;
}
