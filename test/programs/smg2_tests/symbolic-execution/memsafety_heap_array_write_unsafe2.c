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

    if (nondet > 9 || nondet < -1) {
        free(array);
        return 0;
    }

    array[nondet] = 0;    // UNSAFE for MemSafety
    free(array);
	return 0;	
}