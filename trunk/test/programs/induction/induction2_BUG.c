// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail();

int main() {
	int x = 0;
	while (1) {
		x += 1;
		if (x == 2) {
			x = 0;
		}
		if (x >= 1) {
			__assert_fail();
			return 1;
		}
	}
	return 0;
}
