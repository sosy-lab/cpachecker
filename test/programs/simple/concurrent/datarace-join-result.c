// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// pthread_join sets res to g, so the write through buf races with the read of g.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
extern int pthread_join(pthread_t __th, void **__thread_return);

int g;

void *reader(void *arg) {
  int r = g;
  return 0;
}

void *get_g(void *arg) {
  return &g;
}

int main() {
  pthread_t r, t;
  void *res = 0;
  pthread_create(&r, 0, reader, 0);
  pthread_create(&t, 0, get_g, 0);
  pthread_join(t, &res);
  int *buf = res;
  if (buf != 0) {
    *buf = 1;
  }
  pthread_join(r, 0);
  return 0;
}
