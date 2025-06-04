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

unsigned int summation(unsigned char i){
  unsigned char j = 0;
  unsigned int sum = 0;
  while(j < i){
    sum = j + sum;
    j++;
  }
  return sum;
}

int main(){
  unsigned char n = 0;
  while(1){
    unsigned char n =  __VERIFIER_nondet_uchar();
    if(n > 0){
      break;
    }
  }
  unsigned int s = summation(n);
  if(s != (n*(n+1))/2){
    reach_error();
    return -1;
  }
  return s;
}
