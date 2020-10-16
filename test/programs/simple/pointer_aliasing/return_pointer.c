// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

void test(int x) {
	if (!x) {
ERROR: goto ERROR;
	}
}

int* makeIndirection(int i) {
	int si = sizeof(int);
	int* p;

	p = malloc(si);

	if (p == 0) {
END: goto END;
	}

	*p = i;
	return p;
}

void main() {
	int a = 42;
	int* p = 0;

	p = makeIndirection(a);

	test(p != 0);
	test(*p == a);
}

