// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>

#ifdef BLAST_AUTO_1
int VERDICT_SAFE;
int CURRENTLY_UNSAFE;
#else
int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;
#endif

//kzalloc - allocates memory initialized to zero
//calloc is it's equivalent in user space

struct A {
	int *a;
	int *b;
};

int main(void) {
#ifdef BLAST_AUTO_1
	struct A *x = (struct A*)calloc(0, sizeof(struct A));
#else
	struct A *x = (struct A*)malloc(sizeof(struct A));	
#endif
	//printf("x->a=%d\n", (int)x->a);
	assert(x->a == 0);
	return 0;
}

