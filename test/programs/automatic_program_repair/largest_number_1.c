// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

// Error on line 21, should be arr[i] > largest
int main() {
    int first = __VERIFIER_nondet_int();
    int second = __VERIFIER_nondet_int();
    int third = __VERIFIER_nondet_int();
    int arr[] = {first,second,third};
    int largest = first;
    int len = 3;

    for (int i = 1; i < len; i++) {
      if(arr[i] < largest) {
        largest = arr[i];
      }
    }

    if (largest >= arr[0] && largest >= arr[1] && largest >= arr[2]){
        goto EXIT;
    } else {
        goto ERROR;
    }

  EXIT: return 0;
  ERROR: return 1;
}