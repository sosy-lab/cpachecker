// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f(int x) {
	if (x) {
		return 1;
	} else {
		return 0;
	}
}

void main() {
	int a = 0;
	if (a) {
		f(a);
	}

	int b;
	b = f(1);
	if (b) {
ERROR:		goto ERROR;
	}
}
