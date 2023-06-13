// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void foo() { }

void main() {
	int p;
	int x;

	while(1) { // outer loop
		foo();

		if (p) {
			goto loop1;
		} else {
			goto loop2;
		}

loop1:
		foo();
loop2:
		foo();

		if (x) {
			goto loopexit;
		}

		if (p) {
			goto loop1;
		} else {
			goto loop2;
		}
loopexit:
		foo();
	}
}
