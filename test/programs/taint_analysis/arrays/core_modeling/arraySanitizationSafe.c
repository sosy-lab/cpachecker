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

    // Individual elements of the array `d` cannot be sanitized when `d` is tainted.
    // neither by setting them public
    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_set_public(d[i], 1);
    }

    // or by redefining them
    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        d[i] = 1;
    }

    // The array and its elements are expected to remain tainted
    __VERIFIER_is_public(d, 0);
    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_is_public(d[i], 0);
    }

    // A new definition of an array, like d = {1, 2, 3} is not posible in C. Making the sanitization by array-redefinition not possible.
    // Sanitize the whole array `d`
    __VERIFIER_set_public(d, 1);

    // array `d` and its components are now expected to be untainted
    __VERIFIER_is_public(d, 1);
    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_is_public(d[i], 1);
    }
}
