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
    int a, b;
    int i, j;
    int *p;

    a = b = 2;
    i = j = 200;
    p = &b;

    foo(&a);

    // `foo()` has a side effect on the value stored in the memory address of `a`.
    // `a` is now expected to be tainted.
    __VERIFIER_is_public(a, 0);

    ((int*) (a+b));

    // No side effects apply to a or b, since the taint assign in foo() is made to an invalid memory address
    __VERIFIER_is_public(a, 0);
    __VERIFIER_is_public(b, 1);

    foo(i + p + j);

    // No side effects apply to i, j or p, since the taint assign in foo() is made to an invalid memory address
    __VERIFIER_is_public(i, 1);
    __VERIFIER_is_public(j, 1);
    __VERIFIER_is_public(p, 1);
}

void foo(int* p) {
    *p = taint();
}

int taint() {
    return __VERIFIER_nondet_int();
}
