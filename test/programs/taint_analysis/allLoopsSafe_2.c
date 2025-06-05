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
    int a, b, c, tainted, i;
    int a1, b1, c1, i1;
    a = b = 0;
    c = tainted = __VERIFIER_nondet_int();
    int argc = __VERIFIER_nondet_int();

    for (i = 0; i < argc; ++i) {
        a++;
//        while (b < 500) {
        while (b < 50) {
            b++; // we add this assignment that ensures that the loop terminates, unlike the original benchmark allLoopsSafe_1.c
            do {
                c = argc + 1; // t(c) = t(argc) = T
            } while (c < argc);
        }
        b+=tainted; // t(b) = t(tainted) = T
    }

    // The expected taint is now different from the original benchmark, because
    // now the taint flow in the loop body reaches the publicity-check.
    __VERIFIER_is_public(a, 1);
    __VERIFIER_is_public(b, 0);
    __VERIFIER_is_public(c, 0);

    a1 = b1 = c1 = 0;
    for (i1 = 0; i1 < argc; ++i1) {
        a1++;
//        while (b1 < 500) {
        while (b1 < 50) {
            do {
                c1 = argc + 1; // t(c1) = t(argc) + t(1) = T + U = T
            } while (c1 < argc);
            b1+=2;
        }
    }

    __VERIFIER_is_public(a1, 1);
    __VERIFIER_is_public(b1, 1);
    __VERIFIER_is_public(c1, 0);
}
