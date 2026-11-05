// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// pthread_join writes res, which races with the read of res.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
extern int pthread_join(pthread_t __th, void **__thread_return);

void *res;

void *reader(void *arg) {
  void *r = res;
  return 0;
}

void *worker(void *arg) {
  return 0;
}

int main() {
  pthread_t r, t;
  pthread_create(&t, 0, worker, 0);
  pthread_create(&r, 0, reader, 0);
  pthread_join(t, &res);
  pthread_join(r, 0);
  return 0;
}
