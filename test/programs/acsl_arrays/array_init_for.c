// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int);
extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

int main() {
    int size = __VERIFIER_nondet_int();
    __VERIFIER_assume(size >= 11);

    int A[size];

    for (int i = 0; i < size; i++) {
        A[i] = 42;
    }

    if ((A[10]!=42)){
        __assert_fail("0", "array_init_for.c", 26, "main");
    }
}