// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/2.ta_sizeof_alignof.c

// We use an extern function sizeOf to, conceptually, emulate the original sizeof.
// The actual functionality of sizeof is not relevant here, but the information flow is.

extern int sizeOf(int number);
extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x, y, z, w;

    // All variables start untainted
    x = __VERIFIER_nondet_int();
    y = __VERIFIER_nondet_int();
    z = __VERIFIER_nondet_int();
    w = __VERIFIER_nondet_int();

    // `sizeof(int)` assigns an untainted value to `x`. `x` becomes sanitized
    x = sizeOf(1);
    __VERIFIER_is_public(x, 1);

    // `sizeof(x + z)` assigns a tainted value to `y`; `y` remains tainted
    y = sizeOf(x + z);
    __VERIFIER_is_public(y, 0);

    // `sizeof(y)` assigns a tainted value to `z`; `z` remains tainted
    z = sizeOf(y);
    __VERIFIER_is_public(z, 0);

    // `sizeof(x * x)` assigns an untainted value to `w` as `x` is now untainted
    w = sizeOf(x * x);
    __VERIFIER_is_public(w, 1);

    // Expected taint analysis result: `x` and `w` are public, `y` and `z` are tainted.
    // No information disclosure here, but we expect that the analysis propagates taint to all
    // LHS elements when in the RHS there is a tainted variable. Because of that we expect the
    // total sum to be tainted and with that a property violation here.
    __VERIFIER_is_public(x + y + z + w, 1);
}
