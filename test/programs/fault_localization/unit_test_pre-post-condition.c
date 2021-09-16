// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int isPrime(int n){
	for(int i = 2; i < n/2 + 1; i++){
		if(n % i == 0) return 0;	
	}
	return 1;
}

/** 
  Calculate all prime factors of a given number.
  Example: prime factors of 420 are {2, 2, 3, 5, 7} because
           2 * 2 * 3 * 5 * 7 = 420 and 2,3,5,7 are prime.
  */
int main(){

	// Calculate prime factors of number;
	int number = __VERIFIER_nondet_int();
	int copyForCheck = number;
	if (number <= 0) {
		// Tell user that a positive number is required.
		// This is not consider to be an error.
		goto EXIT;
	}

	int test = 1;
	for(int i = 2; i <= number; i++){
		if (number % i == 0 && isPrime(i)) {
			// Multiply all prime factors to test
			test *= i;
			// Reset i to restart computation with new number
			number = number / i;
			i = 2;
		}
	}

	// POST-CONDITION check if test equals number 
	// (test should equal the product of all found prime factors)
	if(test != copyForCheck) {
		goto ERROR;	
	}

EXIT: return 0;
ERROR: return 1;
}
