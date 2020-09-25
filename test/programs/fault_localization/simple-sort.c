// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

#define TRUE 0
#define FALSE 1

int isSorted(int a[], int len){
	// check if the array is sorted
	for(int i = 0; i < len-1; i++) {
		if(a[i] > a[i+1]) {
			return FALSE;
		}	
	}
	return TRUE;
}

/** Sort any 3-dimensional array ascending */
int main(){
	// sort any array of size 3 in ascending order
	// let the user input 3 numbers that should be sorted.
	int first = __VERIFIER_nondet_int();
	int second = __VERIFIER_nondet_int();
	int third = __VERIFIER_nondet_int();
	int a[] = {first,second,third};

	// length of array
	int len = 3;

	// current position.
	int i = 0;

	while(!isSorted(a,len)) {
		// swap entries if not sorted
		int buff = a[i];
		a[i] = a[i+1];
		a[i+1] = buff;
		i++;
		if (i == len-1) {
			i = 0;		
		}	
	}

	//POST-CONDITION check if the array is sorted?
	if (a[0] <= a[1] && a[1] <= a[2]) {
		goto EXIT;	
	} else {
		goto ERROR;	
	}


EXIT: return 0;
ERROR: return 1;

}
