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

int maxArray(int* a, unsigned int l, unsigned int n) {
  int m = a[0];
  int j = 0;
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
  unsigned int length = 50;
  while(i < length){
    arr[i] = __VERIFIER_nondet_uint();
    i++;
  }
  unsigned char k = __VERIFIER_nondet_uchar();
  k = k % length;
  return maxArray(arr,length,k);
}
