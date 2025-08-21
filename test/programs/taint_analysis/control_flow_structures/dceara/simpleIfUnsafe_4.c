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

int main(int argc) {

    int b = __VERIFIER_nondet_int();
    int c = __VERIFIER_nondet_int();

    if (argc) {
        b = 3; // t(c) = U
        c = argc;
    } else {
        c = 4; // t(c) = T
    }

    // t(b) = U + T = T
    __VERIFIER_is_public(b, 1);

    // t(c) = T + U = T
    __VERIFIER_is_public(c, 1);

    // t(b + c) = T + T = T
    __VERIFIER_is_public(b + c, 1);
}
