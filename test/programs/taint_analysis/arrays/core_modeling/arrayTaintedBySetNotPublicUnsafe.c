// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = 1;

    int d[1];
    d[0] = x; // d is public

    // Make the array not public
    __VERIFIER_set_public(d, 0);

    // Property violation expected
    __VERIFIER_is_public(d, 1);
}
