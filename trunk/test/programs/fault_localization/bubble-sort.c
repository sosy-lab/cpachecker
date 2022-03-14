// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(){

    int first = 5;
    int second = 4;
    int third = 6;
    int a[] = {first,second,third};
    int length = 3;

    int tmp = 0;

	for (int i = 0; i < length; i++) {
		for (int j = 1; j < length - i ; j++) { // FIX: j = 0
			if (a[j] > a[j+1]) {
                		tmp = a[j];
              			a[j] = a[j+1];
              			a[j+1] = tmp;
          		}
      		}
   	}

	//POST-CONDITION check if array a is sorted
	if(a[0] <= a[1] && a[1] <= a[2]) {
		goto EXIT;
	} else {
		goto ERROR;
	}

EXIT: return 0;
ERROR: return 1;
}
