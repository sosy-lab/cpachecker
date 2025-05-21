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

    if (1) {
        a = 3;
    } else {
        b = 4; // unreachable --> b expected to remain tainted
    }

    // Information-flow violation expected, because a is expected to be public
    __VERIFIER_is_public(a, 0);
}
