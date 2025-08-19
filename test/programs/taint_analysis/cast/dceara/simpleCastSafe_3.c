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
} t2;

typedef struct {
    int a;
    int b;
    int c;
} t3;

int main() {
    t1* s1;
    t2 s2;
    t3* s3;
    s2.a = 10;
    s2.b = 10;
    s1 = (t1*)(&s2);        // T(s1) = U
    s3 = (t3*)(&s2);        // T(s3) = T

    __VERIFIER_is_public(s1, 1);
    __VERIFIER_is_public(s3, 1);
}
