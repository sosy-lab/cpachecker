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

int main(int argc) {
    int x = bar(0, 1, 2);
    int y = bar(0, 1, argc);

    __VERIFIER_is_public(x, 1);
    __VERIFIER_is_public(y, 0);

    return 0;
}

int foo(int x3) {
    return 10;
}

int bar(int x1, int y1, int z1) {
    return x1 + foobar(z1, x1, y1);
}

int foobar(int x2, int y2, int z2) {
    return x2 + foo(y2);
}

