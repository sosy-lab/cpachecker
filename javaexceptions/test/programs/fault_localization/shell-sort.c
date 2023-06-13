// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// cannot be analyzed by cpachecker
void shell_sort(int a[], int size) {
	int i, j;
	int h = 1;
 	do {
		h = h*3+1;
 	} while (h <= size);
 
	do {
		h /= 3;
		for(int i = h; i < size; i++) {
			int v = a[i];
			for (j = i; j >= h && a[j-h] > v; j-=h)
				a[j] = a[j-h];
			if (i != j) a[j] = v;
		}
	} while (h!=1);
}

int main(){
	int a[] = {14,11};
	shell_sort(a, 2);
	for(int i = 0; i < 1;i++) {
		if(a[i] > a[i+1]){
			goto ERROR;
		}
	}
	
EXIT: return 0;
ERROR: return 1;
}

