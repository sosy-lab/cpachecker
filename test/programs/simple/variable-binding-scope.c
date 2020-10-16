// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
	int x = 5;
	{
		int x = x;
		// the inner x has a non-deterministical value

		if (x == 0) {
			// so this error location is reachable
ERROR:			goto ERROR;
		}
	}
}
