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

void main(int argc) {
//    int b = args;
    int b = __VERIFIER_nondet_int();
    int a = __VERIFIER_nondet_int();
    int x;

    if (b) {
        x = __VERIFIER_nondet_int();
    } else {
        x = 222;
    }

    __VERIFIER_is_public(x, 0);

    foo(a);
    __VERIFIER_is_public(a, 0);

    foo(b);
    __VERIFIER_is_public(b, 0);

}

int foo(int n) {
    int x;
    if (n) {
        x = 2;
        return n * foo(n - 1);
    } else {
        x = __VERIFIER_nondet_int();
        return 1;
    }
}
