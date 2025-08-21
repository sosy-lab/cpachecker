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

typedef struct {
    int a;
    char c1;
    char c2;
    char c3;
    char c4;
} t1;

typedef struct {
    int a;
    int b;
    int c;
} t4;

typedef struct {
    int a;
    int b;
} t2;

int main(int argc) {
    t1* s1;
    t2 s2;
    t4* s4;
    s2.a = 10;
    s2.b = 10;
    s1 = (t1*)(&s2); // T(s1) = U
    s4 = (t4*)(&s2); // T(s4) = T
    // -> Conservative approach that taints s4, because, unline s1, s4 gives access to all the fields of s2.
    // Probably it assumes that the cast of s1 is not even valid.

    __VERIFIER_is_public(s1, 1);
    __VERIFIER_is_public(s4, 0);

    return 0;
}
