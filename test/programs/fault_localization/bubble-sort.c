// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

void bubblesort(int array[], int length) {
	int tmp = 0;

	for (int i = 0; i < length ; i++) {
		for (int j = 1; j < length - i ; j++) {
			if (array[j] > array[j+1]) {
                		tmp = array[j];
              			array[j] = array[j+1];
              			array[j+1] = tmp;
          		}
      		}
   	}
}

int main(){

	int first = __VERIFIER_nondet_int();
	int second = __VERIFIER_nondet_int();
	int third = __VERIFIER_nondet_int();
	int a[] = {first,second,third};
	bubblesort(a, 3);

	//POST-CONDITION check if array a is sorted
	if(a[0] <= a[1] && a[1] <= a[2]) {
		goto EXIT;
	} else {
		goto ERROR;
	}


EXIT: return 0;
ERROR: return 1;
}
