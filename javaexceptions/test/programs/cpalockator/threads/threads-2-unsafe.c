// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

//Test should check, how the tool handle the skipped variables due to annotations
int tmp;
int global;

typedef int pthread_mutex_t;
typedef unsigned long int pthread_t;
typedef int pthread_attr_t;
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int pthread_create(pthread_t *thread_id , pthread_attr_t const   *attr , void *(*func)(void * ) ,
                          void *arg ) ;

pthread_mutex_t mutex;

struct mystruct {
	int* a;
} S;

typedef struct mystruct __my;

__my *S2;

int initialize() {
	global = 1;
    S.a = 1;
    S2->a = 1;
}

void* control_function(void* arg) {
	initialize();
	tmp = 0;
}

int main() {
    pthread_t thread;
    __my A;
    
	A.a[0] = 1;
	pthread_create(&thread, 0, &control_function, 0);
	pthread_mutex_lock(&mutex);
    tmp = 0;
    S.a = 0;
    S2->a = 0;
	pthread_mutex_unlock(&mutex);
}

