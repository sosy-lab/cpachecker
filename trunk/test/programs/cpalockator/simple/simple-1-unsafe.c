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

int gvar;
pthread_mutex_t mutex;

void unsafe_func(void) {
	gvar = 1;
	return 0;
}

void safe_func(void) {
	int b;
	pthread_mutex_lock(&mutex);
	b = gvar;
	pthread_mutex_unlock(&mutex);
}

void main(void) {
	unsafe_func();
	safe_func();
	return;
}
