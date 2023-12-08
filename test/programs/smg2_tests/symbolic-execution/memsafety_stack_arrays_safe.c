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

// null initialized array size 0 == null
int garr_0[] = {};

// null initialized array size 10
int garr_10[10] = {};

int static_array[10] = {1,2,3,4,5,6,7,8,9,10};

int static_const_array[] = {1,2,3,4,5,6,7,8,9,10};

// This file is invalid in strict C11 but ok for GCC
// Note: check this file with -Wpedantic -std=c11 to see that all empty initializers are not part of the C standard (they are in C23 ;D)
// Check with -Wgnu-empty-initializer to see the extension used
int main() {

    int array[10] = {1,2,3,4,5,6,7,8,9,10};

    int array_const[] = {1,2,3,4,5,6,7,8,9,10};

    // TODO: variable sized arrays once we support them

    // Access the arrays with a nondet that is however in the range of the arrays
    int nondet = __VERIFIER_nondet_int();
    int undeclared;

    if (nondet > 9 || nondet < 0) {
        return 0;
    }
    if (undeclared > 9 || undeclared < 0) {
        return 0;
    }

    // IDEA: return a unknown that is in the range/set whatever of the values in the array?
    int ret = garr_10[nondet];

    ret = static_array[nondet];

    ret = static_const_array[nondet];

    ret = array[nondet];

    ret = array_const[nondet];

    ret = garr_10[undeclared];

    ret = static_array[undeclared];

    ret = static_const_array[undeclared];

    ret = array[undeclared];

    ret = array_const[undeclared];

    // Change the nondets a little
    nondet = nondet + nondet;
    nondet = nondet / 2;
    nondet++;
    nondet--;

    ret = garr_10[nondet];

    ret = static_array[nondet];

    ret = static_const_array[nondet];

    ret = array[nondet];

    ret = array_const[nondet];

    undeclared = undeclared - 1;
    if (undeclared < 0) {
        return 0;
    }

    ret = garr_10[undeclared];

    ret = static_array[undeclared];

    ret = static_const_array[undeclared];

    ret = array[undeclared];

    ret = array_const[undeclared];

    int array9[9] = {1,2,3,4,5,6,7,8,9};

    ret = array9[undeclared];

    // Now write to the arrays and see that the writes are valid
    array9[undeclared] = 0;

    garr_10[undeclared] = 0;

    static_array[undeclared] = 0;

    static_const_array[undeclared] = 0;

    array[undeclared] = 0;

    array_const[undeclared] = 0;


    garr_10[nondet] = 0;

    static_array[nondet] = 0;

    static_const_array[nondet] = 0;

    array[nondet] = 0;

    array_const[nondet] = 0;

    // SAFE
	return 0;	
}
