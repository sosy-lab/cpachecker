// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail();

int main() {
	int x1 = 0;
	int x2 = 0;

	int i = 1;
	while (1) {
		if (i == 1) {
			x1++;
		} else if (i == 2) {
			x2++;
		}

		i++;
		if (i == 3) {
			i = 1;
			if (!(x1 == x2)) {
				__assert_fail();
				return 1;
			}
		}
	}
}
