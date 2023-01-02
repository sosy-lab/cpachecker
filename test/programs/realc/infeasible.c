// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main()
{

	int a = 0;
	int b = 100;

	b = rand(); // nicht deterministische zufallszahl
	a = 40;

	if (a + b == 42 && b == 7)
	{
ERROR: return;
	}


	return;
} 
