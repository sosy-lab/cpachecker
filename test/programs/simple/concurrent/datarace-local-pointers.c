// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Both threads access `g` through local pointers, so these accesses are not thread-local.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);

int g;
int *shared = &g;
int gap;

void *writer(void *arg) {
  int *n = shared;
  *n = 1;
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, writer, 0);
  gap = 0;
  int *m = shared;
  int r = *m;
  gap = 1;
  return 0;
}
