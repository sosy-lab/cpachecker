// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_assume(int);

int main() {
	int x;
	__VERIFIER_assume(x == 1);
	if (x != 1) {
ERROR:
		goto ERROR;
	}
}
