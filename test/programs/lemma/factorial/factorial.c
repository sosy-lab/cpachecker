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
extern unsigned __VERIFIER_nondet_uchar();

unsigned int factorial(unsigned char i){
  unsigned char j = 1;
  unsigned int fac = 1;
  while(j < i){
    j++;
    fac = j*fac;
  }
  return fac;
}

int main(){
  unsigned char n =  __VERIFIER_nondet_uchar();
  unsigned int f = factorial(n);
  if(f < n){
    reach_error();
    return -1;
  }
  return f;
}
