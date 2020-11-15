// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

/** Find a number greater than seed that is divisble by 3 */
int generateNumberDiv3(int seed){
	int old;
	do{
		old = seed;
		seed++;
	} while (old%3 != 0);
	return seed;
}

int main(){
	// Generate a number that is divisible by 3
	int seed = __VERIFIER_nondet_int();
	int div3 = generateNumberDiv3(seed);

	// POST-CONDITION check if the number is divisble by 3
	if(div3 % 3 != 0) 
		goto ERROR;
EXIT: return 0;
ERROR: return 1;
}
