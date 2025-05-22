// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);

int global1;
int global2;

// TODO: implement taint of parameters passed to main
int main(int argc) {
    int a, b, c, x, y;
    int z, t;
    a = 2;            // T(a) = U
    b = argc;         // T(b) = T
    __VERIFIER_set_public(b, 0); // Remove after implemented todo

    c = 33;           // T(c) = U

    x = foo(a, b);    // T(x) = T
    __VERIFIER_is_public(x, 0);

    y = foo(a, c);    // T(y) = U
    __VERIFIER_is_public(y, 1);

    z = foo_rec(a);   // T(z) = U
    __VERIFIER_is_public(z, 1);

    t = foo_rec(b);   // T(t) = T
    __VERIFIER_is_public(t, 0);

    foo_global1(1000); // T(global1) = U
    __VERIFIER_is_public(global1, 1);

    foo_global2(b); // T(global2) = T
    __VERIFIER_is_public(global2, 0);
}


int foo(int x, int y) {
    return x + y;
}

int foo_rec (int n) {
    if (n == 0)
        return 1;
    else
        return n * foo_rec(n-1);
}

void foo_global1 (int n) {
    if (n != 0) {
        global1 *= n;
        foo_global1(n-1);
    }
}

void foo_global2 (int n) {
    if (n != 0) {
        global2 *= n;
        foo_global2(n-1);
    }
}
