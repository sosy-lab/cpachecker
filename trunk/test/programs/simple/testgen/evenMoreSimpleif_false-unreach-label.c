// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
int a;
int b;
int main() {
	a = __VERIFIER_nondet_int();
	if(a > 6) {
		b = 5;
	} else {
		b = 4;
	}
	if(a < 6) {
		b = 4;
	} else {
		b = 5;
	}

	if (a == 99) {
		return 33;
	}

	if(b == __VERIFIER_nondet_int()) {
		//return 1;
		ERROR: goto ERROR;
	}else{
		return 0;
		//ERROR: goto ERROR;
	}
	return 1;
}
