// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(){
	
	int arr[] = {1,2,3,4,5};
	int x = 1; // is this number contained in arr?
	int l = 0;
	int r = 5 - 1;
	int m;

	int test = -1;
	while (l <= r) {
		// calculate mid
		m = l + (r-l)/2; 
	  
		// Check if x is present at mid 
		if (arr[m] == x) {
			test = 1;
			break;
		}
	  
		// ignore left half   
		if (arr[m] > x)  
			l = m + 1;  	  
		// ignore right half  
		else 
			r = m - 1;  
	}

	//POST-CONDITION check if binary search was correct
	if(1 != test) {
		goto ERROR;
	}

EXIT: return 0;
ERROR: return 1;
}
