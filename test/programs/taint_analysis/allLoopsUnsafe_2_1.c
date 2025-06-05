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
    int a, b, c, tainted, i;
    a = b = 0;
    c = tainted = __VERIFIER_nondet_int();
    int argc = __VERIFIER_nondet_int();

    for (i = 0; i < argc; ++i) {
        a++;
        while (b < 500) {
            b++;
            do {
                c = argc + 1; // t(c) = t(argc) = T
            } while (c < argc);
        }
        b+=tainted; // t(b) = t(tainted) = T
    }

    __VERIFIER_is_public(a, 0);
    __VERIFIER_is_public(b, 1);
    __VERIFIER_is_public(c, 1);
}
