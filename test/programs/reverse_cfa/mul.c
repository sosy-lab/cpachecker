// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main()
{ 
	int a = 3;
	int b = 4; 
	int c = 0;
	c = a * b; 
	if ( c == 12 ) { ERROR: ; } 
}
