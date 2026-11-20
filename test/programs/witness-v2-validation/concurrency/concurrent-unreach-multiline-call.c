// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Like concurrent-unreach.c, but the call creating the thread spans two lines, so its closing
// parenthesis, to which function_enter waypoints point, is not on the line where the call starts.

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
    pthread_create(&t, 0,
                   writer, 0);
    if (x == 1) reach_error();
    pthread_join(t, 0);
    return 0;
}
