// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f() {
	int x;
	x = 0;
	return (x);
}

int main() {
	int y;
	y = f();
	if (y != 0) {
ERROR:
		goto ERROR;
	}
	return (y);
}
