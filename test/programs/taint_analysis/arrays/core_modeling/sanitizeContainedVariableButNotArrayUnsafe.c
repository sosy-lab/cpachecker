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
    int x = 1;
    int y = __VERIFIER_nondet_int();

    int d[2];
    d[0] = x;

    // d hasn't been tainted.
    // This behavior is verified by the benchmark program examplePublicArray*.c
    __VERIFIER_is_public(d, 1);

    // the taint flows from y to d with the following call.
    d[1] = y;

    // This behavior is verified by the benchmark program exampleSecretArray*.c
    __VERIFIER_is_public(d, 0);

    // this call is going to sanitize the variable y
    __VERIFIER_set_public(y, 1);

    // This behavior is verified by the benchmark program exampleSetSecretVariablePublic*.c
    __VERIFIER_is_public(y, 1);

    // The sanitization of `y` will not sanitize the array d.
    // d is considered tainted and therefore a property violation is expected here
    __VERIFIER_is_public(d, 1);
}
