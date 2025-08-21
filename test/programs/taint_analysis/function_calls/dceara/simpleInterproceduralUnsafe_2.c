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
    int a = 2; // t(a) = U
    int b = __VERIFIER_nondet_int(); // t(b) = T
    int* p = __VERIFIER_nondet_int(); // t(p) = T
    int c = foo(a, b); // t(c) = t(foo(a, b)) = t(a + b) = t(a) + t(b) = U + T = T
    int d = foo(a, argc); // t(d) = t(a + argc) = t(a) + t(argc) = U + T = T
    int e = foo(a, foo(a, d)); // t(e) = t(a + (a + d)) = t(a) + t(a) + t(d) = U + U + T = T
    int f = *p; // t(f) = t(p) = T

    __VERIFIER_is_public(e, 1);
}

int foo(int x, int y) {
    return x + y;
}
