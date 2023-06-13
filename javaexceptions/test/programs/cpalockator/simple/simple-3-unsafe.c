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

int gvar;
pthread_mutex_t mutex;

void inner(void) {
	int b = __VERIFIER_nondet_int();
	pthread_mutex_lock(&mutex);
	if (b)
		pthread_mutex_unlock(&mutex);
	else 
		b = b+5;
	gvar = 10;
}

void main(void) {
	inner();
}
