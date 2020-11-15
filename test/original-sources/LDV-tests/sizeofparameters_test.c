// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>
#include <assert.h>

int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;

void foo();

int globalSize;

int 
main(int argc, char* argv[]) {
	long int a;
	globalSize=sizeof(a);
	foo(a);
	return 0;
}

void foo(int a) {
	assert(sizeof(a)==globalSize);
}
