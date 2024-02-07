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

    int * array = malloc(10 * sizeof(int));

    for (int i = 0; i < 10; i++) {
        array[i] = i + 1;
    }

    int nondet = __VERIFIER_nondet_int();

    if (nondet > 9 || nondet < 0) {
        free(array);
        return 0;
    }

    int unknown_value = array[nondet]; // SAFE

    assert(unknown_value != 10);  // UNSAFE as it might be 10
    free(array);

    // SAFE for MemSafety, UNSAFE for ReachSafety
	return 0;	
}
