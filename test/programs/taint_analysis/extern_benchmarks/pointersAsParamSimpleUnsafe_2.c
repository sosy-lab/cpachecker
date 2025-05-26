// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int a = 2;
    int *p;

    foo(&a);

    // `foo()` has a side effect on the value stored in the memory address of `a`.
    // `a` is now expected to be tainted.
    // Property violation expected
    __VERIFIER_is_public(a, 1);
}

void foo(int* p) {
    *p = taint();
}

int taint() {
    return __VERIFIER_nondet_int();
}
