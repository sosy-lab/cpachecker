// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>

extern int __VERIFIER_nondet_int(void);
extern void abort(void);

int main() {

    int * array = malloc(sizeof(int) * 10);

    int nondet = __VERIFIER_nondet_int();
    int undeclared;

    if (nondet > 9 || nondet < 0) {
        free(array);
        return 0;
    }
    if (undeclared > 9 || undeclared < 0) {
        free(array);
        return 0;
    }

    int value = array[nondet];
    value = array[undeclared];

    value = *(array + nondet);
    value = *(array + undeclared);

    int * array12 = malloc(sizeof(int) * 12);

    nondet++;
    ++nondet;

    undeclared = undeclared + 2;    

    value = array12[nondet];
    value = array12[undeclared];

    value = *(array12 + nondet);
    value = *(array12 + undeclared);

    free(array);
    free(array12);

    // SAFE
	return 0;	
}
