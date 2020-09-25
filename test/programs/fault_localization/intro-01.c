// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main(){

	//calculate 10%3 manually
	int a = 10;
	int b = 3;

	while(a >= 0) 
		a -= b;
	a = -a - b;

	//POST-CONDITION check if manual computation is right (10%3 = 1)
	if(a!=1)
		goto ERROR;
	

EXIT: return 0;
ERROR: return 1;
}
