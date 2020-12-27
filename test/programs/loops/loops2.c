// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main() {
	int p1;
	int p2;

	while (0) {
		if (p1) {
			goto M;
		}
	}

M:

	while (0) {
		if (p2) {
			goto E;
		}
	}

E:	return;

}
