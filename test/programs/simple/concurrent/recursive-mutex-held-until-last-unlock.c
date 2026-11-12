// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// m is recursive and locked twice by main,
// so the write in main after the first unlock is still protected.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[36]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
typedef union { char __size[4]; int __align; } pthread_mutexattr_t;
typedef union { char __size[24]; long int __align; } pthread_mutex_t;
enum { PTHREAD_MUTEX_TIMED_NP, PTHREAD_MUTEX_RECURSIVE_NP };
enum { PTHREAD_MUTEX_RECURSIVE = PTHREAD_MUTEX_RECURSIVE_NP };
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
extern int pthread_mutexattr_init(pthread_mutexattr_t *__attr);
extern int pthread_mutexattr_settype(pthread_mutexattr_t *__attr, int __kind);
extern int pthread_mutex_init(pthread_mutex_t *__mutex,
    const pthread_mutexattr_t *__mutexattr);
extern int pthread_mutex_lock(pthread_mutex_t *__mutex);
extern int pthread_mutex_unlock(pthread_mutex_t *__mutex);

int x;
pthread_mutex_t m;

void *t1(void *arg) {
  pthread_mutex_lock(&m);
  x = 1;
  pthread_mutex_unlock(&m);
  return 0;
}

int main() {
  pthread_t t;
  pthread_mutexattr_t attr;
  pthread_mutexattr_init(&attr);
  pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
  pthread_mutex_init(&m, &attr);
  pthread_create(&t, 0, t1, 0);
  pthread_mutex_lock(&m);
  pthread_mutex_lock(&m);
  pthread_mutex_unlock(&m);
  x = 2;
  pthread_mutex_unlock(&m);
  return 0;
}
