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

int main() {

    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();
    int c = __VERIFIER_nondet_int();
    int x = __VERIFIER_nondet_int();

    if (x) {
        if (2) {
            b = 2;
            c = 3;
            // t(a) = T, t(b) = U, t(c) = U, t(x) = T
        } else {
            c = 2;
            // t(a) = T, t(b) = T, t(c) = U, t(x) = T
        }
        // t(a) = T + T = T
        // t(b) = U + T = T
        // t(c) = U + U = U
        // t(x) = T + T = T
    } else {
        c = x;
        // t(a) = T, t(b) = T, t(c) = T, t(x) = T
    }
    // t(a) = T + T = T
    // t(b) = T + T = T
    // t(c) = U + T = T
    // t(x) = T + T = T

    // a is expected to be tainted
    __VERIFIER_is_public(a, 0);

    // b is expected to be tainted
    __VERIFIER_is_public(b, 0);

    // c is expected to be tainted
    __VERIFIER_is_public(c, 0);

    // x is expected to be tainted
    __VERIFIER_is_public(x, 0);
}
