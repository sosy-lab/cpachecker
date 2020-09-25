// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int isDivisible(int number, int divisor) {
	while(!(number < 0)) {
		number -= divisor;
	}
	return 0 == number;
}

int gcd0(int min, int max){
	for(int i = min; i >= 2; i--) {
		if(isDivisible(max, i)) {
			if(isDivisible(min, i)) {
				return i;
			}
		}	
	}
	return 1;
}

int gcd(int number1, int number2){
	if(number1 <= 0 || number2 <= 0)
		return -1;
	
	if(number2 > number1) {
		return gcd0(number1, number2);
	}
	return gcd0(number2, number1);
}

/**
  * Calculate the GCD (greatest common divisor) of two positive whole numbers (0 excluded)
*/
int main(){

	// Test input: GCD of 12 and 8 (= 4)
	int number1 = 12;
	int number2 = 8;

	int result = gcd(number1, number2);

	// There is no positive whole number solution for negative values or 0
	if(result == -1) 
		goto EXIT;
	
	// POST-CONDITION check if gcd(12,8) equals 4
	if(result != 4)
		goto ERROR;
	
	

EXIT: return 0;
ERROR: return 1;
}
