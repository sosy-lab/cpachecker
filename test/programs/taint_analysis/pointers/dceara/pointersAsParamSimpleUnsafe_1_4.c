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
    int a, b;
    int i, j;
    int x, y, z;
    int *p;

    a = b = 2;
    i = j = 200;
    p = &b;

    x = foo(&a);

    // (foo:p = &a -> a) was tainted by the `tainted` var. a (and therefore &a) are expected to be tainted.
    // property violation expected
    __VERIFIER_is_public(&a, 1);
}

int foo(int* p) {
    int tainted = __VERIFIER_nondet_int();
    *p = tainted;
    int a = 3;
    return 100;
}
