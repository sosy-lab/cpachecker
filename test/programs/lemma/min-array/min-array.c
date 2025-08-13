/*
* This file is part of CPAchecker,
* a tool for configurable software verification:
* https://cpachecker.sosy-lab.org
*
* SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
*
* SPDX-License-Identifier: Apache-2.0
*/
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "linear-inequality-inv-a.c", 2, "reach_error"); }
extern unsigned __VERIFIER_nondet_uint();
extern unsigned __VERIFIER_nondet_uchar();

int minArray(int* a, unsigned int l) {
  int m = a[0];
  int j = 0;
  while(j < l) {
    if(a[j] < m) {
      m = a[j];
    }
    ++j;
  }
  return m;
}

int main() {
  int arr[10000];
  int i = 0;
  unsigned int length = 10000;
  while(i < length){
    arr[i] = __VERIFIER_nondet_uint();
    i++;
  }
  int min = minArray(arr, length);
  for(int k = 0; k < length; k++){
    if(min < arr[k]){
      reach_error();
      return -1;
    }
  }
  return 0;
}
