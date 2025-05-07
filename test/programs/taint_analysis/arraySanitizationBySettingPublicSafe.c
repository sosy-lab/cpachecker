// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern void __VERIFIER_is_public(int variable, int booleanFlag);

// Test that sanitizing an array element works correctly
int main() {
    int x = __VERIFIER_nondet_int();

    int d[1];
    d[0] = x; // `d` is now tainted

    // Sanitize the array `d` by making it public
    __VERIFIER_set_public(d, 1);

    // `d` should be public now. No property violation expected
    __VERIFIER_is_public(d, 1);
}
