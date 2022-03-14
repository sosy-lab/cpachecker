// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f1() {
	int x;
	return x;
}


int main() {
	// Trigger UNINITIALIZED_RETURN_VALUE and UNINITIALIZED_VARIABLE_USED
	int y;
	y = f1();

	return (0);
	ERROR: return (-1);
}
