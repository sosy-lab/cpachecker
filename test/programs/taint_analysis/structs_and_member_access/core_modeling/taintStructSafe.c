// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

// Define a struct for a triple
struct Triple {
    int a;
    int b;
    int c;
};

int main() {
    int x = 1;
    int y = __VERIFIER_nondet_int();
    int z = 2;
    struct Triple t = {x, y, z};

    // No property violation expected
    __VERIFIER_is_public(t, 0);
    __VERIFIER_is_public(t.a, 0);
    __VERIFIER_is_public(t.b, 0);
    __VERIFIER_is_public(t.c, 0);
    __VERIFIER_is_public(&t, 0);
}
