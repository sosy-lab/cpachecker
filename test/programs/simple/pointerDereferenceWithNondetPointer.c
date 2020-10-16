// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void * __VERIFIER_nondet_pointer();
extern void __VERIFIER_error();

int a = 0;
int *p2 = &a;

int main() {

    int *p = (int*) __VERIFIER_nondet_pointer();
    *p = 5;

    if (a > 0) {
ERROR:
        __VERIFIER_error();
    }
}
