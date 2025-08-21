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

int main(int argc) {
    int a, b, d, e;
    a = b = d = e = __VERIFIER_nondet_int(); // all tainted
    a = 2;
    b = argc;                                //T(b) = G(argc)
    d = a + b + d;                           //T(d) = U + G(argc) + T = T
    e = a + b + b;                           //T(d) = U + G(argc) + G(argc) = G(argc)

    __VERIFIER_is_public(a, 1);
    __VERIFIER_is_public(b, 0);
    __VERIFIER_is_public(d, 0);
    __VERIFIER_is_public(e, 0);

    return 0;
}
