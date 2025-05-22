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
// TODO
void strcpy(char* dest, char* source) {

    if (__VERIFIER_is_public(dest, 0)
            || (__VERIFIER_is_public(dest, 1) && __VERIFIER_is_public(source, 1))) {
        dest = source;
    }
}

int main() {
    char* src;
    char* dst;

//    dst = __VERIFIER_nondet_char();
    dst = 2;
    src = 0;
    src = __VERIFIER_nondet_char();
    strcpy(dst, src); // dest tainted => operation allowed
    strcpy(dst, src); // dest untainted + src untainted => operation allowed
    src = __VERIFIER_nondet_char();
    strcpy(dst, src); // dest untainted + src tainted => operation not allowed
}
