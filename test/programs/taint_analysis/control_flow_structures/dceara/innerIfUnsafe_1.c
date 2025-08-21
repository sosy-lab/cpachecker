// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main(int argc, int argc2) {

    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();
    int c = __VERIFIER_nondet_int();

    if (argc) { // t(argc) = T
        if (2) {
            b = 2; // t(b) = U
            c = 3; // t(c) = U
        } else {
            c = 2; // t(c) = U
        }
    } else {
        c = argc2; // t(c) = T
    }

    // t(a) = T
    __VERIFIER_is_public(a, 1);

    // t(b) = T + U = T
    __VERIFIER_is_public(b, 1);

    // t(c) = U + T = T
    __VERIFIER_is_public(c, 1);
}
