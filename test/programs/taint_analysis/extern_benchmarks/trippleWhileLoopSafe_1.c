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

    while (a < 10) {
        while (b < 10) {
            while (c < x) {
                c++;
            }
            b++;
        }
        a++;
    }

    while (a1 < 10) {
        while (b1 < x) {
            while (c1 < 10) {
                c1++;
            }
            b1++;
        }
        a1++;
    }

    while (a2 < 10) {
        while (b2 < x) {
            while (c2 < 10) {
                c2++;
            }
            b2++;
        }
        a2 = b;
    }

    // TODO: analysis does not terminate
    __VERIFIER_is_public(a2, 0);
    __VERIFIER_is_public(b2, 0);
    __VERIFIER_is_public(c2, 0);
}
