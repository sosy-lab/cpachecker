// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
extern double SQRT(double number1);
extern int __VERIFIER_is_public(int variable, int booleanFlag);
extern void __VERIFIER_set_public(int variable, int booleanFlag);

int main() {
    double x = 2;
    double y = 2;

    // asume some operation tainted y
    __VERIFIER_set_public(y, 0);

    double z = x * y;
    double w = SQRT(z);

    __VERIFIER_is_public(w, 1);
}
