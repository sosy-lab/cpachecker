// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include<assert.h>

void foo(int* r) {
	assert(*r == 42);
	assert(!(*r != 42));
}

void main() {
	int a = 42;
	foo(&a);
}
