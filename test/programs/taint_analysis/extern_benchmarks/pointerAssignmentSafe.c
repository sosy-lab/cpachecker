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
// TODO: In this benchmark there is things to do
int main() {
    int *p, a, *q, b, c;
    p = __VERIFIER_nondet_int();
    c = __VERIFIER_nondet_int();

    a = 2;

    // AMPER Unary operator not supported by CPAchecker
//    p = &a;

    b = *p;
    *q = c;

    __VERIFIER_is_public(a, 1);
    __VERIFIER_is_public(b, 0);
    __VERIFIER_is_public(c, 0);
    __VERIFIER_is_public(p, 0);

    // Should q be tainted or public? -> Investigate
//    __VERIFIER_is_public(q, 0);
}
