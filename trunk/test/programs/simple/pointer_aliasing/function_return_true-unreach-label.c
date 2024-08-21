// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include<assert.h>

struct s {
	int *p;
};

int a = 42;

int* foo() {
	return &a;
}

void main() {
	struct s s;
	int *r = &a;
	assert(*r == 42);
	assert(!(*r != 42));

	s.p = foo();
	assert (*s.p == 42);
	assert (!(*s.p != 42));
}

