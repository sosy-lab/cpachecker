// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = __VERIFIER_nondet_int();
    int y = 1;
    int z;

    // x + y is expected to be tainted by x and the + operator
    __VERIFIER_is_public(x + y, 0);

    // y - x is expected to be tainted by x and the - operator
    __VERIFIER_is_public(y - x, 0);

    // y * x is expected to be tainted by x and the * operator
    __VERIFIER_is_public(y * x, 0);

    // y / x is expected to be tainted by x and the / operator
    __VERIFIER_is_public(y / x, 0);

    // y % x is expected to be tainted by x and the % operator
    __VERIFIER_is_public(y % x, 0);

    // Expression is expected to be tainted by x and the mixed operation
    __VERIFIER_is_public((x * y + y / x) % y, 0);
}
