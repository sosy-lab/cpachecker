// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {

    // All variables start tainted
    int b = __VERIFIER_nondet_int();
    int c = __VERIFIER_nondet_int();

    if (1) {
        b = 3; // unreachable
    } else {
        c = 4;
    }

    // b is expected to remain tainted
    __VERIFIER_is_public(b, 1);

    // c is expected to be public
    __VERIFIER_is_public(c, 0);

    // b + c is expected to be tainted by c
    __VERIFIER_is_public(b + c, 0);
}
