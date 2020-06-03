// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int i;
int j;

extern int __VERIFIER_nondet_int();

void f(int *p) {
	*p = 1;
	j = 1;
}

int main() {
	if (__VERIFIER_nondet_int()) {
		f(&i);
	}
	if (i != 0 && j == 0) {
ERROR:
		return 1;
	}
}
