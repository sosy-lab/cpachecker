// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error();
extern int __VERIFIER_nondet_int();

int add_one(int i) {
	int j = i + 1;
    return j;
}

void main() {
    int i = __VERIFIER_nondet_int();
    i = add_one(i);
    i = 5;
    if (i == 1) {
ERROR:
        __VERIFIER_error();
    }
}