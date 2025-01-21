/*
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
*/

extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "linear-inequality-inv-a.c", 2, "reach_error"); }
extern unsigned __VERIFIER_nondet_uint();
extern unsigned __VERIFIER_nondet_uchar();



int max(int a, int b) {
    return a>=b ? a:b;
}

int maxArray (int *a, int l) {
  int m = a[0];

  for(int i = 0; i < l; i++) {
    m = max(m, a[i]);
  }
  return m;
}

int main() {

  unsigned char l = __VERIFIER_nondet_uchar();
  if (l < 1){
    return 0;
  }
  int a[l];
  for(int i=0;i<20;i++){
    a[i] = __VERIFIER_nondet_uint();
  }
  // int l = 20;
  int m = maxArray(a,l);

  if(m < a[0]) {
    reach_error();
    return 1;
  }
  
  if (m < a[l-1]) {
    reach_error();
    return 1;
  }

  return 0;
}
