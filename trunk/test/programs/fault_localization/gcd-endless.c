// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int isDivisible(int number, int divisor) {
	// e.g number = 10 and divisor = 3;
	// div = 3
	int div = number / divisor;
	// result = 3 * 3 = 9 != 10 -> not divisible
	int result = number * div; //FIX(1/2): divisor * div
	return result == number;
}

int gcd0(int min, int max){
	for(int i = max/2 + 1; i >= 1; i--) {
	        // Eventually, we call isDivisible(12, 12) which is true but 12 is not gcd(32, 12).
		if(isDivisible(min, i)) { // FIX(2/2): isDivisible(min, i) && isDivisible(max, i)
			return i;
		}	
	}
	return 1;
}

int gcd1(int number1, int number2){
	if (number1 < 0 || number2 < 0) {
		return -1;
	}
	if(number1 == 0 || number1 == number2) {
		return number2;
	}
	if(number2 == 0) {
		return number1;
	}
	
	if(number2 > number1) {
		return gcd0(number1, number2);
	}
	return gcd0(number2, number1);
}

// Test a limit of MAX-SAT and ERR-INV: both techniques are bad in recognizing missing function calls.
int main(){

	int number1 = 32; // 2 * 2 * 2 * 2 * 2
	int number2 = 12; // 2 * 2 * 3

	int gcd = gcd1(number1, number2);
	if(gcd != 4)
		goto ERROR;
	
	

EXIT: return 0;
ERROR: return 1;
}
