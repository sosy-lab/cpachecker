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
    int a, b, c, d, tainted, i;
    int a1, b1, c1, d1, i1;
    a = b = d = 0;
    c = tainted = __VERIFIER_nondet_int();
    int argc = __VERIFIER_nondet_int();

    for (int i = 0; i < argc; ++i) {
        a++;
        while (b < 500) {
            do {
                c = argc + 1;
            } while (c < argc);
        }
        b+=tainted;
    }

    // TODO: double check the expected taint states
    __VERIFIER_is_public(a, 1);
    __VERIFIER_is_public(b, 0);
    __VERIFIER_is_public(c, 0);
    __VERIFIER_is_public(d, 1);

    a1 = b1 = c1 = d1 = 0;
    for (int i1 = 0; i1 < argc; ++i1) {
        a1++;
        while (b1 < 500) {
            do {
                c1 = argc + 1;
            } while (c1 < argc);
        }
        b1+=2;
    }

    __VERIFIER_is_public(a1, 1);
    __VERIFIER_is_public(b1, 1);
    __VERIFIER_is_public(c1, 1);
    __VERIFIER_is_public(d1, 1);
}
