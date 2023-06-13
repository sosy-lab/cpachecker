// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

//Test checks how the analysis may handle returning from function, which change sharedness of its parameter
//In this test there should be a race only on c
int global;

typedef int pthread_mutex_t;
typedef unsigned long int pthread_t;
typedef int pthread_attr_t;
typedef int size_t;
extern void* malloc(size_t size);
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int pthread_create(pthread_t *thread_id , pthread_attr_t const   *attr , void *(*func)(void * ) ,
                          void *arg ) ;
                          
pthread_mutex_t mutex;
                          
int f(int** a, int **b) {
  *a = &global;
  return 0;
}

int access() {
  int** c;
  int** d;
  c = malloc(sizeof(int*));
  *c = malloc(sizeof(int));
  d = malloc(sizeof(int*));
  *d = malloc(sizeof(int));
  
  f(c, d);
  //now it is global
  //**c = 1;
  //still local
  **d = 2;
}

void* control_function(void* arg) {
	access();
}

void* control_function2(void* arg) {
	pthread_mutex_lock(&mutex);
	access();
	pthread_mutex_unlock(&mutex);
}

int main() {
    pthread_t thread, thread2;
	pthread_create(&thread, 0, &control_function, 0);
	pthread_create(&thread2, 0, &control_function2, 0);
}
