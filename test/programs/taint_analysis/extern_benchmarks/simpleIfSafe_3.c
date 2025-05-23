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

    int x = __VERIFIER_nondet_int();
    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();

    if (x) { // with standalone x the if-branch is analysed first, with x == 0 the else-branch goes first
        a = 3;
        // t(a) = U, t(b) = T
    } else {
        a = 1;
        b = 4;
        // t(a) = U, t(b) = U
    }

    // t(a) = U + U = U -> a expected to be untainted
    __VERIFIER_is_public(a, 1);

    // t(b) = T + U = T -> b expected to be tainted
    __VERIFIER_is_public(b, 0);

    // a + b is expected to be tainted
    __VERIFIER_is_public(a + b, 0);

}
