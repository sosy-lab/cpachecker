// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*
 * The ideal verdict differs from the ideal one: state is covered and usage is lost
 * The problem also is at visualization stage.
 */

typedef int pthread_mutex_t;
typedef unsigned long int pthread_t;
typedef int pthread_attr_t;
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int pthread_create(pthread_t *thread_id , pthread_attr_t const   *attr , void *(*func)(void * ) ,
                          void *arg ) ;

extern int dummy_func() ;
extern int __VERIFIER_nondet_int();

int global;
pthread_mutex_t mutex;

void* func(void* arg) {
  global++;
}

void* condition(void* arg) {
  inner(0);
}

int inner(int a) {
  int b = a + 1;
  if (b == 1) {
    return 0;
  } else {
	  pthread_mutex_lock(&mutex);
	  global++;
	  pthread_mutex_unlock(&mutex);
  }
  dummy_func();
  return 1;
}

int main() {
  pthread_t thread1, thread2;
  pthread_create(& thread1, 0, & func, 0);
  pthread_create(& thread2, 0, & condition, 0);
}
