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
    int a1, b1, c1, i1;

    a1 = b1 = c1 = 0;
    for (int i1 = 0; i1 < argc; ++i1) {
        a1++;
        while (b1 < 500) {
            do {
                c1 = argc + 1;
            } while (c1 < argc);
        }
        b1+=2;
    }

    __VERIFIER_is_public(a1, 0);
    __VERIFIER_is_public(b1, 0);
    __VERIFIER_is_public(c1, 0);
}
