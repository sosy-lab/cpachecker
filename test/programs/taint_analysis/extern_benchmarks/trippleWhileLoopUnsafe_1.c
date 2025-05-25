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
    int a, b, c;
    int a1, b1, c1;
    int a2, b2, c2;
    a = c = 0;
    b = __VERIFIER_nondet_int();
    a1 = b1 = c1 = 0;
    a2 = b2 = c2 = 0;
    int x = __VERIFIER_nondet_int();

    while (a < 3) {
        while (b < 3) {
            while (c < x) {
                c++;
            }
            b++;
        }
        a++;
    }

    __VERIFIER_is_public(a, 0);
    __VERIFIER_is_public(b, 1);
    __VERIFIER_is_public(c, 0);

    while (a1 < 3) {
        while (b1 < x) {
            while (c1 < 3) {
                c1++;
            }
            b1++;
        }
        a1++;
    }

    __VERIFIER_is_public(a1, 0);
    __VERIFIER_is_public(b1, 0);
    __VERIFIER_is_public(c1, 0);

    while (a2 < 3) {
        while (b2 < x) {
            while (c2 < 3) {
                c2++;
            }
            b2++;
        }
        a2 = b;
    }

    __VERIFIER_is_public(a2, 0);
    __VERIFIER_is_public(b2, 1);
    __VERIFIER_is_public(c2, 0);
}
