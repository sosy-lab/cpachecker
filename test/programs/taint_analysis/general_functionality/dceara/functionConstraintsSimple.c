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

void strcpy_1(char* dest, char* source) {
    dest = source;
}

int main() {
    char* src;
    char* dst;

    dst = __VERIFIER_nondet_int();
    src = 0;
    strcpy(dst, src); // dest tainted => operation allowed
    __VERIFIER_is_public(dst, 1);

    strcpy(dst, src); // dest untainted + src untainted => operation allowed
    __VERIFIER_is_public(dst, 1);

    src = __VERIFIER_nondet_int();
    strcpy(dst, src); // dest untainted + src tainted => operation not allowed
    __VERIFIER_is_public(dst, 0);
}
