// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Test that sanitizing an array element works correctly
int main() {
    int a = __VERIFIER_nondet_int();

    int d[2];
    d[0] = a; // d is now tainted

    // Sanitize the array by making it public
    __VERIFIER_set_public(d, 1);

    // Should be public now
    __VERIFIER_is_public(d, 1);
}
