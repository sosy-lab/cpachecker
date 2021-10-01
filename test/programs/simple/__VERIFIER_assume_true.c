// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_assume(int);

int main() {
	int i;
	__VERIFIER_assume(i == 2);
	if (i != 2) {
ERROR:
		return 1;
	}
	return 0;
}
