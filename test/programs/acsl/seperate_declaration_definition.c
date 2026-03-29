// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint(void);

/*@ ensures \result == h * w; */
int area(int h, int w);

/*@ requires h >= 0;
    requires w >= 0;
*/
int area(int h, int w){
  int a = h * w;
  return a;
}

int main(){
  int x = __VERIFIER_nondet_uint();
  int y = __VERIFIER_nondet_uint();
  int a = area(x, y);
  if (a != x*y) {
    ERROR: return -1;
  }
  return 0;
}

