// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {

	int a = 0;
	int b = -2;
	int x = 1;
	x = x + a;   //  1
	x = x + b;   // -1
	x = x + 2;   //  1
	x = x - 2;   // -1
	x = x + 0;   // -1
	x = x + -2;  // -3

	if (x < 0)
		goto ERROR;

EXIT: return 0;
ERROR: return 1;
}
