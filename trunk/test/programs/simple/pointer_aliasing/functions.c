// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>

void f(int* p) {
	*p = 2;
}

int g() {
	return 3;
}


int j = 10;

int* h() {
	return &j;
}

void main() {
	int i = 0;
	int* q = &i;
	*q = 1;

	f(q);
	assert(*q == 2);
	assert(i == 2);

	*q = g();
	assert(i == 3);

	int* r = &i;
	r = h();
	assert(*r == 10);
}
