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

    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();

    if (argc) {
        a = 3; // t(a) = U, t(b) = T
    } else {
        b = 4; // t(a) = U, t(b) = U
    }

    // t(a) = T + U = T
    __VERIFIER_is_public(a, 0);

    // t(b) = T + U = T
    __VERIFIER_is_public(b, 1);

    __VERIFIER_is_public(a + b, 1);
}
