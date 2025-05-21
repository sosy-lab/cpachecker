// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

// We use an extern function sizeOf to, conceptually, emulate the original sizeof.
// The actual functionality of sizeof is not relevant here, but the information flow is.

extern int sizeOf(int number);
extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {

    // All variables start tainted
    int x = __VERIFIER_nondet_int();
    int y = __VERIFIER_nondet_int();
    int z = __VERIFIER_nondet_int();
    int w = __VERIFIER_nondet_int();

    // `sizeof(1)` assigns an untainted value to `x`. `x` is now expected to be public
    x = sizeOf(1);
    __VERIFIER_is_public(x, 1);

    // The taint flows from `z` to `y`. `y` is expected to remain tainted
    y = sizeOf(x + z);
    __VERIFIER_is_public(y, 0);

    // The taint flows from `y` to `z`; `z` is expected to remain tainted
    z = sizeOf(y);
    __VERIFIER_is_public(z, 0);

    // `sizeof(x * x)` assigns an untainted value to `w` as `x` is now untainted.
    // `b` is expected to be public
    w = sizeOf(x * x);
    __VERIFIER_is_public(w, 1);

    // Expected taint analysis result: `x` and `w` are public, `y` and `z` are tainted.
    // The combined expression is expected to be tainted.
    __VERIFIER_is_public(x + y + z + w, 0);
    // Note that the function sizeof is not returning sensitive information.
    // Regardless, due to overapproximation, the taint is expected to be propagated to variables
    // that are not actually containing sensitive information.
}
