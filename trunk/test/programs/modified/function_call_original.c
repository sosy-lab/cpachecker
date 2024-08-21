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

void loop(int a) {
	// b is gone now
	int q;
	for(int i; i<1000000; i++) {
		q = a*i;
	}
}

int check(int val) {
	return val > 0;
}

void main() {
    int i = __VERIFIER_nondet_int();
    i = add_one(i);
    loop(add_one(i));
    i = 5;
    if (check(i)) {
ERROR:
        __VERIFIER_error();
    }
}