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

void main() {
	void* p1;
	void* p2;
	int si = sizeof(int);

	p1 = malloc(si);
	p2 = malloc(si);

	test(p1 != p2);

END_PROGRAM: exit();
}
