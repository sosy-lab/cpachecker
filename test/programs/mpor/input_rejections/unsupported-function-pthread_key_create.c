// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned long int pthread_t;
typedef unsigned int pthread_key_t;
union pthread_attr_t
{
  char __size[36];
  long int __align;
};
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create (pthread_t *__restrict __newthread,
      const pthread_attr_t *__restrict __attr,
      void *(*__start_routine) (void *),
      void *__restrict __arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));
extern int pthread_key_create (pthread_key_t *__key,
          void (*__destr_function) (void *))
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));

void * start_routine(void *arg)
{
  const int x = 42;
}
int main()
{
    pthread_t id1;
    pthread_create(&id1, (void*)0, start_routine, (void*)0);
    pthread_key_t key;
    int r = pthread_key_create(&key, (void*)0);
}
