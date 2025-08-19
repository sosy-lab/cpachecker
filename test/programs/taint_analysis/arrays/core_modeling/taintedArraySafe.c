// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);

int main() {
    int x = 1;
    int y = __VERIFIER_nondet_int();
    int z = 1;

    // Uninitialized array declaration
    int d[3];

    // Addition of a non-tainted variable to a non-tainted array
    d[0] = x;

    // array `d` and its components are expected to be still public
    __VERIFIER_is_public(d, 1);

    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_is_public(d[i], 1);
    }

    // Addition of a tainted variable to a non-tainted array
    d[1] = y;

    // array `d` and its components are now expected to be tainted
    __VERIFIER_is_public(d, 0);

    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_is_public(d[i], 0);
    }

    // Addition of a non-tainted variable to a tainted array
    d[2] = z;

    // The array and its elements are still expected to be tainted
    __VERIFIER_is_public(d, 0);

    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_is_public(d[i], 0);
    }

    // Note that for the analysis the added element d[2] is expected to be tainted, because d is tainted, but the variable `z` itself must remain public
    __VERIFIER_is_public(z, 1);

    // Other supported assignments:
    // Declared array with immediate initialization: taint should flow directly from `y` to array `a`
    int a[3] = {x, y, z};

    // array `a` and its components are expected to be tainted
    __VERIFIER_is_public(a, 0);

    for (int i = 0; i < sizeof(a) / sizeof(a[0]); i++) {
        __VERIFIER_is_public(a[i], 0);
    }

    // Compound literal: pass the array directly to the public-state check
    __VERIFIER_is_public((int []){x, y, z}, 0);
}
