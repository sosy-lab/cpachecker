// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
int global = 0;

int assign() {
	int **p;
    int *s;
    int t;
    
    s = &global;
    p = &s;
    
    //unsafe
    **p = 1;
    
    //Not an unsafe
    *p = &t;
    
    //Not an unsafe
    **p = 1;
}

void* control_function1(void* arg) {
	pthread_mutex_lock(&mutex);
	assign();
	pthread_mutex_unlock(&mutex);
}

void* control_function2(void* arg) {
	assign();
}

int main() {
    pthread_t thread, thread2;
	pthread_create(&thread, 0, &control_function1, 0);
	pthread_create(&thread2, 0, &control_function2, 0);
	
}
