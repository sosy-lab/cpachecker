// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

/*@ requires b >= 0; */

/*@ ensures \result == a * b; */
int multiply (int a, int b){
  int c = 0;
  for(int i = 0 ; i < b; i++){
    c = c + a;
  }
  return c;
}

int main() {
  int x = __VERIFIER_nondet_int();
  int y = -1;
  while(y < 0){
    y = __VERIFIER_nondet_int();
  }
  int z = multiply(x, y);
  if(z != x * y) ERROR: return 1;
  
  return 0;
}
