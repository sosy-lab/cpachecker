// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);
extern void f(int var1, int var2);
extern int g(int var);

// how do we define global variables in C (if possible). When, e.g., a call to a function with unknown body is made, should we taint the global variables?

int main() {
    int x = __VERIFIER_nondet_int();
    int y = 1;
    int z = 1;

    z = (x, y);
    // Although we normally taint every LHS when the corresponding RHS contains a tainted var, this is an exception.
    // x alone have no effect on y, and therefore, (x, y) returns `y` without any influence of `x`. `y` stays untainted in this case.
    __VERIFIER_is_public(z, 1);

    // (y, x) returns x and z should become tainted
    z = (y, x);
    __VERIFIER_is_public(z, 0);

    // This check is equivalent to the last one above
    __VERIFIER_is_public((y, x), 0);

    // Since f could be a function that internally makes the taint flow from x to y,
    // the analysis taints all the parameters from `f` when one of its arguments is tainted
    z = (f(x, y), y);
    __VERIFIER_is_public(z, 0);

    // This check is equivalent to the last one above
    __VERIFIER_is_public((f(x, y), y), 0);

    // We sanitize y and z, now x is the only defined source of taint
    __VERIFIER_set_public(y, 1);
    __VERIFIER_set_public(z, 1);

    // One of the arguments of f, itself being an expression, is still supported and should taint the variables correctly
    __VERIFIER_is_public((f(g(x + z), y), y), 0);

    // We sanitize y and z, now x is the only defined source of taint
    __VERIFIER_set_public(y, 1);
    __VERIFIER_set_public(z, 1);

    // Can be better illustrated with pre definitions:
    // taint flows from x to a
    int a = g(x + z);
    __VERIFIER_is_public(a, 0);

    // taint flows from a to y and b
    int b = (f(a, y), y);
    __VERIFIER_is_public(b, 0);
    __VERIFIER_is_public(y, 0);

    // No property violation expected
    __VERIFIER_is_public((b, y), 0);
}
