// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/** 
  * Calculate the maximum scoring sequence.
  * The input is an array of integers.
  * The output is the highest sum of consecutive elements in the array.
  * Example: for array = [2, -1, 7, -5, 2] the output is 8 because 2 + (-1) + 7 = 8
  * There is no other partial sequence wich sums up to a higher value.
*/
int main(){

	// The result is 8 because 5 + (-2) + 5 = 8
	int a[] = {-2, 5, -2, 5};
	// Will store the overall maxscore
	int test = 0;
	
	// 4 = length of array
	// Loop through every possible consecutive sequence
	for (int i = 1; i <= 4; i++) {
		for (int j = i - 1; j < 4; j++) {
			int s = 0;
			// Calculate the sum in the given interval
			for (int k = i; k <= j; k++) {
				s = s + a[k - 1];
			}
			// Is the sum greater than the current maxscore?
			if (s > test) {
				test = s;
			}
		}
	}

	//POST-CONDITION check if the result is equal to 8
	if(test != 8) {
		goto ERROR;
	}

EXIT: return 0;
ERROR: return 1;
}
