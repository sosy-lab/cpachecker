// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>

void f() {
};

extern int constant();

void main() {
	int i1;
	int i2;
	i1 = constant();
	f();
	i2 = constant();
	assert(i1 == i2);
}
