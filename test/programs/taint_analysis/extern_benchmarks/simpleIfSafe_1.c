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
    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();

    if (0) {
        a = 3; // unreachable
    } else {
        b = 4;
    }

    // a is expected to remain tainted
    __VERIFIER_is_public(a, 0);

    // b is expected to be public
    __VERIFIER_is_public(b, 1);

    // a + b is expected to be tainted by a
    __VERIFIER_is_public(a + b, 0);

// TODO: What to do when main returns tainted objects? -> Property violation?
//    return a + b;
}
