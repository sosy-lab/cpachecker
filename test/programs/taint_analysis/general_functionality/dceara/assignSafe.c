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

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {

    // All variables start tainted
    int a = __VERIFIER_nondet_int();
//    int b = __VERIFIER_nondet_int();
//    int c = __VERIFIER_nondet_int();
//    int d = __VERIFIER_nondet_int();
//    int e = __VERIFIER_nondet_int();
//    int f = __VERIFIER_nondet_int();
//    int g = __VERIFIER_nondet_int();

    // `sizeof(1)` assigns an untainted value to `x`. `x` is now expected to be public
    int b;
    b = sizeof(a);
    __VERIFIER_is_public(b, 0);
}
