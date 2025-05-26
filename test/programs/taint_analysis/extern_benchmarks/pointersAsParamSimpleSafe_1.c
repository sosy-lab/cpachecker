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

    // p points to b and therefore not tainted
    __VERIFIER_is_public(p, 1);

    x = foo(&a);

    // main:p still points to b and b has not been modified. p is expected to remain public
    __VERIFIER_is_public(p, 1);

    // x is expected to be public, because it stores an untainted constant value returned by foo()
    __VERIFIER_is_public(x, 1);

    // foo:p = &a was tainted by the `tainted` var. a (and therefore &a) are expected to be tainted.
    __VERIFIER_is_public(&a, 0);

    // But `foo` has a side effect on the value stored in the memory address of `a`.
    // `a` is now expected to be tainted.
    __VERIFIER_is_public(a, 0);

    y = foo((int*) (a+b));

    // y is expected to be public, because it stores an untainted constant value returned by foo()
    __VERIFIER_is_public(y, 1);

    // No side effects apply to a or b, since the taint assign in foo() is made to an invalid memory address
    __VERIFIER_is_public(a, 0);
    __VERIFIER_is_public(b, 1);

    z = foo(i + p + j);

    // z is expected to be public, because it stores an untainted constant value returned by foo()
    __VERIFIER_is_public(z, 1);

    // No side effects, since the taint assign in foo() is made to an invalid memory address
    __VERIFIER_is_public(a, 0);
    __VERIFIER_is_public(b, 1);

}

int foo(int* p) {
    int tainted = __VERIFIER_nondet_int();
    *p = tainted;
    int a = 3;
    return 100;
}
