// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f() {
	int ret;
	return ret;
}

int main() {
	int x = f();
	int y = f();
	// x and y may be equal here, so this error location has to be reachable
	if (x != y) {
ERROR:
		goto ERROR;
	}
}
