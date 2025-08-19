// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int g (int x) {
    return 2*x;
}

// f propagates the taint only via `x` variable
int f (int x, int y) {
    return g(x);
}

int main() {
    int a = 2;
    int b = __VERIFIER_nondet_int();

    int z = f(a, b);
    // z is not expected to be tainted, because the taint of `b` do not reach the return statement
    __VERIFIER_is_public(z, 1);

    z = f(b, a);
    // f(b,a) is expected to be tainted, because the taint of `b` reaches the return statement
    __VERIFIER_is_public(z, 0);

    // Same public-state result expected as in the first case, but the non-tainted argument in a binary expression
    __VERIFIER_is_public(f(a + 1, b), 1);

    // Same public-state result expected as in the second case, but with the tainted argument in a binary expression
    __VERIFIER_is_public(f(a % b, a), 0);

    // The expression is expected to be public, because the first argument of the outer f is untainted.
    __VERIFIER_is_public(f(a + 2, f(a * b, 1)), 1);

    // The expression is expected to be tainted, because the first argument of the outer f is tainted.
    __VERIFIER_is_public(f(f(a * b, 1), a + 2), 0);

    // The expression is expected to be tainted. In this case the taint does not depend on f, but on b being directly accesible in the check
    __VERIFIER_is_public(b - f(a, b), 0);
}
