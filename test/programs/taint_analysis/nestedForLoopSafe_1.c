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
    int a = 1;
    int x = __VERIFIER_nondet_int();

    for (int i = 0; i < x; ++ i) {
        a = a * x;
        for (int j = 0; x; j++) {
            a = x;
        }
    }

    // TODO: Nested For-Loop not working fine
    // t(a) = T
    __VERIFIER_is_public(a, 0);
}
