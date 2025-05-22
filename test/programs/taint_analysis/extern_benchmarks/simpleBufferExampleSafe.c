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
    int* buf1;
    int buf2[20];
    int* clean1;
    int clean2[20];

    int x = __VERIFIER_nondet_int();
    buf1[10] = x;
    // TODO: Analysis must recognize the tainted array by initialize with a source
//    buf1[10] = __VERIFIER_nondet_int();
    buf2[10] = x;
//    buf2[10] = __VERIFIER_nondet_int();
    clean1[10] = 2;
    clean2[10] = 2;

    __VERIFIER_is_public(buf1, 0);
    __VERIFIER_is_public(buf2, 0);
//    __VERIFIER_is_public(clean1, 1);
//    __VERIFIER_is_public(clean2, 1);
}
