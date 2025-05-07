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

int main() {
    int x = __VERIFIER_nondet_int();
    int y = __VERIFIER_nondet_int();

    // z is tainted
    int z = x + y;
    __VERIFIER_is_public(z, 0);

    // sanitize x by redefinition
    x = 1;
    __VERIFIER_is_public(x, 1);

    // z is still tainted
    z = x + y;
    __VERIFIER_is_public(z, 0);

    // sanitize y by the sanitization function
    __VERIFIER_set_public(y, 1);
    __VERIFIER_is_public(y, 1);

    // z is expected not tainted
    z = x + y;
    __VERIFIER_is_public(z, 0);
}
