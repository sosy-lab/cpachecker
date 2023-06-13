// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
	int a = 6;


	for(a = 0; a < 6; ++a) {
		
	}	
	if(!(a == 6 )) {
		ERROR: goto ERROR;
	}
	return 1;
}
