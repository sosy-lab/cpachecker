// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
	int a = 6;
	int b = 6;
	int c = 6;
	int d = 8;

	if(a == 6) {
		a = 7;
	} else {
		a = 4;
	}

	if(c == 5) {
				
	}

	if (a == 99) {
		return 33;
	}

	if(!(a == 6 && b == 6 && c == 6 && d == 6 )) {
		a = 33;
	} else {
		ERROR: goto ERROR;
	}
	return 1;
}
