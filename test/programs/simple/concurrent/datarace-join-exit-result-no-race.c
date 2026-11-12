// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// pthread_exit sets the result to &other, so the write through buf does not race with the read of g.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
extern int pthread_join(pthread_t __th, void **__thread_return);
extern void pthread_exit(void *__retval) __attribute__((__noreturn__));

int g;
int other;

void *reader(void *arg) {
  int *q = &g;
  int r = *q;
  return 0;
}

void exit_with_other(void) {
  pthread_exit(&other);
}

void *get_other(void *arg) {
  exit_with_other();
  return &g;
}

int main() {
  pthread_t r, t;
  void *res = 0;
  pthread_create(&r, 0, reader, 0);
  pthread_create(&t, 0, get_other, 0);
  pthread_join(t, &res);
  int *buf = res;
  if (buf != 0) {
    *buf = 1;
  }
  pthread_join(r, 0);
  return 0;
}
