// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {

    int x = 5;

    int d[1];

    // Taint flows into array `d`
    d[0] = x;

    // Property violation expected
    __VERIFIER_is_public(d, 0);
}
