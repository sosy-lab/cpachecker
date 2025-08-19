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

int main(int argc) {
    int a2, b, b2;
    a2 = b2 = 2;
    b = __VERIFIER_nondet_int();

    while (a2 < 10) {
        while (b2 < argc) {
            b2 = 1;
        }
        a2 = b;
    }

    __VERIFIER_is_public(a2, 0);
    __VERIFIER_is_public(b, 0);
    __VERIFIER_is_public(b2, 0);
}
