// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int a, b;
    int a1, b1;
    a = b = 0;
    a1 = b1 = __VERIFIER_nondet_int();
    int x = __VERIFIER_nondet_int();

    while (a < 10) {
        while (b < 20) {
            b++;
        }
        a++;
    }

    __VERIFIER_is_public(a, 1);
    __VERIFIER_is_public(b, 1);

    a1 = 0;
    while (a1 < 10) {
        while (b1) { // <- with this it works, but it is not terminating when using a condition like b1 < x
            b1++;
        }
        a1++;
    }

    __VERIFIER_is_public(a1, 1);
    __VERIFIER_is_public(b1, 0);
}
