// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(){

	//numbers in array correspond to ASCII code
	int string[] = {72,101,108,108,111,32,87,111,114,108,100,33};
	int upperCase[] = {65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78 ,79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90};
	int lowerCase[26];
	for(int i = 0; i < 26; i++) {
		lowerCase[i] = upperCase[i] + 32;
	}

	for(int i = 0; i < 12; i++){
		int changed = 0;
		for(int l = 0; l < 26; l++) {
			int up = upperCase[l];
			int lo = lowerCase[l];
			if(string[i] == up){
				string[i] = lo;
				changed = 1;
				break;
			}
			if(string[i] == lo){
				string[i] = up;
				changed = 1;
				break;
			}
		}
	}

	// POSTCONDITION check if string matches correct
	int correct[] = {104,69,76,76,79,32,119,79,82,76,68,33};
	if(correct[0] == string[0] && correct[1] == string[1] && correct[2] == string[2] && correct[3] == string[3] && correct[4] == string[4] && correct[5] == string[5] && correct[6] == string[6] && correct[7] == string[7] && correct[8] == string[8] && correct[9] == string[9] && correct[10] == string[10] && correct[11] == string[11] && correct[12] == string[12]){
		goto EXIT;
	} else {
		goto ERROR;
	}

EXIT: return 0;
ERROR: return 1;
}
