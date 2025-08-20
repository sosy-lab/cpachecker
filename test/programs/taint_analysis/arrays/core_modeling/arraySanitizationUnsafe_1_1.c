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

    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        d[i] = 1;
    }

    __VERIFIER_is_public(d, 0);
}
