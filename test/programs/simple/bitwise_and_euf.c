// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void f() { }
int main() {
	int a;
	int b;
	int x;
	int y;
	a = 42;
	b = 23;
	x = a & b;
	y = 42;
	y = y & b;
	f(); // force abstraction
	if (x != y) {
ERROR:
		goto ERROR;
	}
}
