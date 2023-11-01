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

// null initialized array size 10
int garr_10[10] = {};

// This file is invalid in strict C11 but ok for GCC
// Note: check this file with -Wpedantic -std=c11 to see that all empty initializers are not part of the C standard (they are in C23 ;D)
// Check with -Wgnu-empty-initializer to see the extension used
int main() {
    int nondet = __VERIFIER_nondet_int();

    if (nondet > 10 || nondet < 0) {
        return 0;
    }

    int value = garr_10[nondet];    // UNSAFE for MemSafety

	return 0;	
}
