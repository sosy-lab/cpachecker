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
	int result = x;
	if (x) {
		result = 0;
	} else {
		result = simple_function(result);
	}
	result = simple_function(result);
	if (!(result == 1 || result == 2)) {
		return 0;
	}
	ERROR: return 1;
}
