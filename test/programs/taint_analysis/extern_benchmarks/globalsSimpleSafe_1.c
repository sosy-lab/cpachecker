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

int global = 2;

int main() {
    __VERIFIER_is_public(global, 1);

    taint_global();

    __VERIFIER_is_public(global, 0);

    untaint_global();

    __VERIFIER_is_public(global, 1);
}

void taint_global() {
    global = __VERIFIER_nondet_int();
}

void untaint_global() {
    global = 100;
}
