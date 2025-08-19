// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = 0;
    int y = __VERIFIER_nondet_int();

    // taint flows to d
    int d[2] = {x, y};

    // Sanitize the whole array `d`
    __VERIFIER_set_public(d, 1);

    // Information-flow violation expected:
    // elements of the array `d` are now expected to be untainted
    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_is_public(d[i], 0);
    }
}
