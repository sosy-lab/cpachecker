// This file is part of the SV-Witnesses repository as regression test:
// https://gitlab.com/sosy-lab/benchmarking/sv-witnesses
//
// SPDX-FileCopyrightText: 2025 The SV-Witnesses Community
//
// SPDX-License-Identifier: Apache-2.0

// Concurrent violation: writer thread writes x=1 before main reads x,
// so 'if (x == 1)' is taken and reach_error() is called.
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

void* writer(void* arg) {
    x = 1;      /* line 35 */
    return 0;
}

int main(void) {
    pthread_t t;
    pthread_create(&t, 0, writer, 0);
    if (x == 1) reach_error();
    pthread_join(t, 0);
    return 0;
}
