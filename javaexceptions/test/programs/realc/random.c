// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main()
{
	int a;
	
	while (1) {
		a = rand() * rand();

		if (a == 41){
			ERROR: return;
		}
	}

	return;
}
