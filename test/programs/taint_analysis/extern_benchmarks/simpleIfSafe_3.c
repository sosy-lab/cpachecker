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

    // TODO: Does not work completely fine. In the if blocks the order still matters.

    // problem, when we here analyse first the branch that violates the property the analyse fails wrongly.
    if (x) { // with standalone x the if-branch is analysed first, with x == 0 the else-branch goes first
        a = 3; // t(a) = U, t(b) = T
    } else {
        b = 4; // t(a) = T, t(b) = U
    }

    // Failure trigger: whenever the branch that contradicts the public-check is explored first

    // with ternary op it works fine
//    a = x ? a : 3;
//    b = x ? b : 4;

    // a + b is expected to be tainted
    __VERIFIER_is_public(a + b, 0);

    // with a does not work as expected
    __VERIFIER_is_public(a, 0); // fails
}
