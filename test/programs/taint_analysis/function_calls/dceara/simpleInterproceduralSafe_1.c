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

int main() {
    int a = 2;
    int b = 2;
    int c = foo(a, b); // t(c) = t (a + b) = t(a) + t(b) = U + U = U

    __VERIFIER_is_public(c, 1);

    return 0;
}

int foo(int x, int y) {
    return x + y;
}
