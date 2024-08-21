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
    return i + 1;
}

int check(int val) {
	return val > 0;
}

void loop(int a, int b) {
	int q;
	for(int i; i<=999999; i++) {
		q = a*i + b;
	}
}

void main() {
    int i = __VERIFIER_nondet_int();
    i = add_one(i);
    loop(add_one(i), 5);
    i = 5;
    if (check(i)) {
ERROR:
        __VERIFIER_error();
    }
}