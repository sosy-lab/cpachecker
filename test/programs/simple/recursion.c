// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void recursive(int i) {
	if (i > 0) {
		i--;
		recursive(i);
	}
}

void main() {
	recursive(10);
}
