// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int isPrime(int n){
	for(int i = 2; i <= n/2 + 1; i++){ // FIX: i < n/2 + 1 or i <= n/2
		if(n % i == 0) return 0;	
	}
	return 1;
}

int main(){

	//Calculate prime factors of number;
	int number = __VERIFIER_nondet_int();
	if (number < 0) {
		//Tell user that a positive number is required
		goto EXIT;
	}

	for(int i = 2; i < number; i++){
		if (number % i == 0 && isPrime(i)) {
			number = number / i;
			i--;
		}
	}

	if(number == 1) {
		goto ERROR;	
	}

EXIT: return 0;
ERROR: return 1;
}
