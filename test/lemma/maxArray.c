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

int maxArray(int* a, int l, int n) {
  int m = a[0];
  int j = 1;
  while(j < l) {
    if(a[j] > m) {
      m = a[j];
    }
    ++j;
  }
  if(m < a[n]) {
    reach_error();
    return -1;
  }
  return m;
}

int main() {
  int arr[50];
  int i = 0;
  int l = 50;
  while(i < l){
    arr[i] = __VERIFIER_nondet_uint();
    i++;
  }
  char k = 0;
  while(1) {
    char k = __VERIFIER_nondet_uchar();
    if(k < l) {
      break;
    }
  }

  return maxArray(arr,l,k);
}
