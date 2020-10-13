// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int __VERIFIER_nondet_int();

int isPrime(int check){
	if(check <= 1){
		return 0;
	}
	// check/2 + 1 for fewer checks. 
	for(int i = 2; i <= check/2+1; i++){
		if(check % i == 0){
			return 0;
		}
	}
	return 1;
}

/** Check if a number is a prime number */
int main() {

	int input = __VERIFIER_nondet_int();
	int check = input % 10; 
	int result = isPrime(check);
	
	/* POST-CONDITION check if the program was able to identify all primes
	   from 0 to 10. The checks below are correct! */
	if(	   (result == 0 && check <= 0) || (result == 0 && check == 1) 
		|| (result == 1 && check == 2) || (result == 1 && check == 3) 
		|| (result == 0 && check == 4) || (result == 1 && check == 5) 
		|| (result == 0 && check == 6) || (result == 1 && check == 7) 
		|| (result == 0 && check == 8) || (result == 0 && check == 9)){
		goto EXIT;
	} else {
		goto ERROR;
	}

	EXIT: return 0;
	ERROR: return 1;
}

