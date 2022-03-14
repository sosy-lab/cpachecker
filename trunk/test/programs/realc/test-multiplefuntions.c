// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int times2(int x)
{
 return x*2;
}

int negate(int x)
{
return x*-1;
}

void check(int x)
{
	if (x < 0){
		ERROR: return;
	}
	else{
		x += 42;
	}
}

int globalVar;

void main() {

	int a, b;
	int * d = &globalVar;
	void * e = malloc(sizeof(int));
	d = (int*)e;
	a = nondet_int() * nondet_int();
	b = -7 * nondet_int();

	a = times2(a);
	a = times2(a);
	b = negate(b);

	*d = nondet_int();
	*d = negate(*d);

	if (a == 44 && b == 7)
	{
		ERROR: return;
	}

	check(a);
	check(b);

	int c = a + b;
	c *= 2;
}


