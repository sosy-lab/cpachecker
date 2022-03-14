// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void test(int x) {
	if (!x) {
ERROR: goto ERROR;
	}
}

void minimal() {
	int a = 123;
	int* p = &a;

	test(*p == 123);

	*p = 50;
	test(a == 50);
	test(*p == 50);
	test(a == *p);
}

void minimal_failing_r6276() {
	int a = 123;
	int* p = &a;

	int** q = &p;

	*p = 50;
	test(a == 50);
	test(*p == 50);
	test(a == *p);
}

void main() {
	minimal();
	minimal_failing_r6276();
}
