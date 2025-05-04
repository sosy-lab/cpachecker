// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int a = __VERIFIER_nondet_int();

    int d[1];
    d[0] = a; // d is now tainted

    // Sanitize the array by making it public
    __VERIFIER_set_public(d, 1);

    // Should be public now, but we assert that it is not,
    // so this benchmark program must return a property violation.
    __VERIFIER_is_public(d, 0);
}
