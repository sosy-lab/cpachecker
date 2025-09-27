// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();


int simple_function(int a) {
	return a + 1;
}

int main() {
	int x = __VERIFIER_nondet_int();
	simple_function(x);
	simple_function(x);
	if (x > 0) {
		int y = x - 1;
		if (y != x - 1) {
		  ERROR: return 1;
	  }
	}
	return 0;
}
