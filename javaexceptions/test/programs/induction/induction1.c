// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail();

int main() {
	int x = 0, y = 0;
	while (1) {
		x++;
		y++;
		if (x != y) {
			__assert_fail();
			return 1;
		}
	}
	return 0;
}
