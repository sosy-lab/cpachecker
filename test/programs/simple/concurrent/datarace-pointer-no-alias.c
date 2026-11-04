// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The write through `p` does not race with the write of `x`, because `p` points to `y`.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);

int x, y;
int *p = &y;

void *writer(void *arg) {
  *p = 1;
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, writer, 0);
  x = 2;
  return 0;
}
