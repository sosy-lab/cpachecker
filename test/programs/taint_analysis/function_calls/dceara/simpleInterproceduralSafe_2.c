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

int main() {
    int argc = __VERIFIER_nondet_int();
    int a = 2;
    int b = __VERIFIER_nondet_int();
    int c = foo(a, b);
    int d = foo(a, argc);
    int e = foo(a, foo(a, d));
    // TODO: handle pointer
    int* p = __VERIFIER_nondet_int();
    int f = *p;

    __VERIFIER_is_public(a, 1);
    __VERIFIER_is_public(b, 0);
    __VERIFIER_is_public(c, 0);
    __VERIFIER_is_public(d, 0);
    __VERIFIER_is_public(e, 0);

    // TODO: Double check the handling of these
    __VERIFIER_is_public(p, 0);
    __VERIFIER_is_public(f, 0);
}

int foo(int x, int y) {
    return x + y;
}
