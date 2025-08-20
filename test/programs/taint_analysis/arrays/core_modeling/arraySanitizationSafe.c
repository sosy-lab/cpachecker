// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = 0;
    int y = __VERIFIER_nondet_int();

    // taint flows to d
    int d[2] = {x, y};

    // Despite sanitizing array elements individually, their taint status will remain tainted
    // until all vars have been untainted. Is at least one tainted var in the array, the array
    // and the rest of the elements are considered tainted.
    // One can sanitize the array by setting all elements public
    __VERIFIER_set_public(d[0], 1);
    __VERIFIER_is_public(d[0], 0);

    __VERIFIER_set_public(d[1], 1);
    __VERIFIER_is_public(d[0], 1);
    __VERIFIER_is_public(d[1], 1);

    __VERIFIER_set_public(d, 0);
    __VERIFIER_is_public(d, 0);

    __VERIFIER_is_public(d[0], 0);
    __VERIFIER_is_public(d[1], 0);

    // or by redefining them
    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        d[i] = 1;
    }

    __VERIFIER_is_public(d, 1);
    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_is_public(d[i], 1);
    }

    // A new definition of an array, like d = {1, 2, 3} is not posible in C. Making the sanitization by array-redefinition not possible.
    // But we can sanitize the whole array
    __VERIFIER_set_public(d, 1);

    // array `d` and its components are now expected to be untainted
    __VERIFIER_is_public(d, 1);
    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_is_public(d[i], 1);
    }
}
