// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef int pthread_mutex_t;
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int __VERIFIER_nondet_int();
void recursion_function(void);


int gvar;
pthread_mutex_t mutex;

void write(void) {
	gvar = 1;
}

void dummy(void) {
	pthread_mutex_lock(&mutex);
	recursion_function();
}

void recursion_function(void) {
	int random = __VERIFIER_nondet_int();
	if (random) {
		dummy();
	}
}

void main(void) {
	dummy();
	//max lock count is arcieved
	write();
	pthread_mutex_unlock(&mutex);
	pthread_mutex_unlock(&mutex);
	pthread_mutex_unlock(&mutex);
	pthread_mutex_unlock(&mutex);
	//lock should be released
	
	write();
}
