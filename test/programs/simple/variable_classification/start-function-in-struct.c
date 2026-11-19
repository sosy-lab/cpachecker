// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The start function comes through a struct field, and the result of pthread_create is unused.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);

int g;

void *f(void *arg) {
  void *r = &g;
  return r;
}

struct task {
  void *(*start)(void *);
};

int main() {
  struct task s = {f};
  pthread_t t;
  int unused = pthread_create(&t, 0, s.start, 0);
  return 0;
}
