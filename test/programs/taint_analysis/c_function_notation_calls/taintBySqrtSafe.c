// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern double SQRT(double number);
extern int __VERIFIER_is_public(int variable, int booleanFlag);
extern void __VERIFIER_set_public(int variable, int booleanFlag);

int main() {
    double x, y, z;

    x = 2;
    y = 2;

    // asume some operation tainted y
    __VERIFIER_set_public(y, 0);

    z = SQRT(x * y);

    // No property violation expected
    __VERIFIER_is_public(z, 0);

    // This line is just to show that the publicity check also supports
    // direct extern function calls.
    __VERIFIER_is_public(SQRT(x * y), 0);
}
