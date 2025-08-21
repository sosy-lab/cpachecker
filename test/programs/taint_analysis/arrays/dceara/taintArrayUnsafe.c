// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main(int argc) {

    int a[234];
    int* b;
    int* c;
    int* d;
    int tainted = __VERIFIER_nondet_int();

    b = __VERIFIER_nondet_int();
    __VERIFIER_is_public(b, 1);

    a[2] = 2;
    __VERIFIER_is_public(a, 0);
    __VERIFIER_is_public(a[2], 0);

    a[tainted] = 354;
    __VERIFIER_is_public(a, 0);

    c[argc] = 23;
    __VERIFIER_is_public(c, 0);

    b[345] = tainted;
    __VERIFIER_is_public(b, 1);

    return 0;
}
