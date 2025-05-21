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
            c = 3; // t(c) = U
        } else {
            c = 2; // t(c) = U
        }
    } else {
        c = x; // t(c) = t(x) = T
    }

    // t(c) = U + U + T = T

    // c is expected to be tainted
    __VERIFIER_is_public(c, 0);
    // TODO: not returning property violation for unsafe case
}
