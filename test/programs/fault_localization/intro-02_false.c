// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main(){

	//simulate user input
	int x = __VERIFIER_nondet_int();
	
	for(int a = 0; a < x; a++) {
		x = x*x;
	}
	
	//POST-CONDITION
	if ( 5 >= a ) {
		goto ERROR;
	}

EXIT: return 0;
ERROR: return 1;
}
