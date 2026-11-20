// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// pthread_cond_wait locks m again before it returns,
// so main cannot change count while the producer holds m.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[36]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
typedef union { char __size[24]; long int __align; } pthread_mutex_t;
typedef union { char __size[4]; int __align; } pthread_mutexattr_t;
typedef union { char __size[48]; long long int __align; } pthread_cond_t;
typedef union { char __size[4]; int __align; } pthread_condattr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);
extern int pthread_join(pthread_t __th, void **__thread_return);
extern int pthread_mutex_init(pthread_mutex_t *__mutex,
    const pthread_mutexattr_t *__mutexattr);
extern int pthread_mutex_lock(pthread_mutex_t *__mutex);
extern int pthread_mutex_unlock(pthread_mutex_t *__mutex);
extern int pthread_cond_init(pthread_cond_t *__restrict __cond,
    const pthread_condattr_t *__restrict __cond_attr);
extern int pthread_cond_signal(pthread_cond_t *__cond);
extern int pthread_cond_wait(pthread_cond_t *__restrict __cond,
    pthread_mutex_t *__restrict __mutex);
extern void reach_error(void);

pthread_mutex_t m;
pthread_cond_t not_empty;
int count;

void *producer(void *arg) {
  for (int i = 0; i < 2; i++) {
    pthread_mutex_lock(&m);
    count++;
    if (count <= 0) {
      reach_error();
    }
    pthread_cond_signal(&not_empty);
    pthread_mutex_unlock(&m);
  }
  return 0;
}

int main() {
  pthread_t t;
  pthread_mutex_init(&m, 0);
  pthread_cond_init(&not_empty, 0);
  pthread_create(&t, 0, producer, 0);
  for (int i = 0; i < 2; i++) {
    pthread_mutex_lock(&m);
    while (count == 0) {
      pthread_cond_wait(&not_empty, &m);
    }
    count--;
    pthread_mutex_unlock(&m);
  }
  pthread_join(t, 0);
  return 0;
}
