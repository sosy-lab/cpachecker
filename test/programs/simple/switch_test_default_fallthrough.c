// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
	int x = 5;
	switch (5) {
	default: // fall-through after default
	case 1:
	case 2:
ERROR:
		return 1;
	case 3:
		return 0;
	}
}	
