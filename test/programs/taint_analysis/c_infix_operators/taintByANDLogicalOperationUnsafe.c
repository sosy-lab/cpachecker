// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x, y, z;
    x = __VERIFIER_nondet_int();
    y = 1;

    // z is expected NOT to be tainted by x and the && operation
    z = y && x;
    // This is one exception to the overapprox. that every tainted RHS taints the LHS.
    // It is a trade-of for letting expressions like x = 1 to sanitize x.
    // This is aceptable, because we know that the logical && do not transmit sensitive information
    // related to the compared variables. It only says whether both of them are not equal to 0.

    // taint violation expected
    __VERIFIER_is_public(z, 0);
}
