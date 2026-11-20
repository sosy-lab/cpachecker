// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Both threads write the pointer variable `p` (issue #1668).

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);

int v1, v2;
int *p;

void *writer(void *arg) {
  p = &v1;
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, writer, 0);
  p = &v2;
  return 0;
}
