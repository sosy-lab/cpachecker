// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

// TODO: Currently the analysis does not support recursion (test 28 in repo)

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = foo(10);
    int y = foo2(20);

    __VERIFIER_is_public(x, 1);
    __VERIFIER_is_public(y, 0);
}

int foo(int n) {
    int r = __VERIFIER_nondet_int();

    if (n > 1)
        r = n * foo(n - 1);
    else
        r = 1;

    return r;
}

int foo2(int n) {
    int r = __VERIFIER_nondet_int();
    int tainted = __VERIFIER_nondet_int();

    if (n > 1)
        r = n * foo2(n - 1);
    else
        r = tainted;

    return r;
}
