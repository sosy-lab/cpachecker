// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/2.ta_sizeof_alignof.c

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern void __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x, y, z, w;

    // All variables start tainted
    x = __VERIFIER_nondet_int();
    y = __VERIFIER_nondet_int();
    z = __VERIFIER_nondet_int();
    w = __VERIFIER_nondet_int();

    // `sizeof(int)` assigns an untainted value to `x`. `x` becomes sanitized
    x = sizeof(int);
    __VERIFIER_is_public(x, 1);

    // taint flows from z to taintedSum
    int taintedSum = x + z;

    // `sizeof(taintedSum)` assigns a tainted value to `y`; `y` remains tainted
    y = sizeof(taintedSum);
    __VERIFIER_is_public(y, 0);

    // `sizeof(y)` assigns a tainted value to `z`; `z` remains tainted
    z = sizeof(y);
    __VERIFIER_is_public(z, 0);

    // Since x is not tainted, taintedMult is not tainted
    int taintedMult = x * x;

    // `sizeof(taintedMult)` assigns an untainted value to `w` as `x` is now untainted
    w = sizeof(taintedMult);
    __VERIFIER_is_public(w, 1);

    // Expected taint analysis result: `x` and `w` are public, `y` and `z` are tainted.
    int totalSum = x + y + z + w;

    // No property violation expected. However, the function sizeof is not returning sensitive information.
    // Due to the overapproximation, the taint is being propagated to variables that are not actually containing sensitive information.
    // A false positive (report tainted, when not really tainted) is expected here.
    __VERIFIER_is_public(totalSum, 0);
}
