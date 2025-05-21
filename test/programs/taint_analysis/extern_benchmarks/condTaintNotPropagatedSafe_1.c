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

    int a = 0;
    int b = 0;
    int c = __VERIFIER_nondet_int();
    int x = __VERIFIER_nondet_int();

    if (a < x) {
        b = 256;
    }

    c = a;

    __VERIFIER_is_public(a, 1);
    __VERIFIER_is_public(b, 1);
    __VERIFIER_is_public(c, 1);
}
