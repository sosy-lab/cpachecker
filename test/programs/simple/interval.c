// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int ext(int p);

int g(a)
{
	int x;

	a = 55;

	if(a > 0)
		x = a + 22;
	else
		x = a - 22;
		
	return x;
}

int main()
{
	int x = 0;
	
	int a = 0;
	int b = 0;
	
	if(x == 0)
	{
		a = 0;
		b = 5;
	}
	else
	{
		a = 10;
		b = 15;
	}
	
	if(a > b)
	{
		x = 1;
	}
	else
	{
		x = 2;
	}

  a = 55;
  
  a = g(55);
}
