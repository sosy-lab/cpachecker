
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

/*@ requires a > 0
    requires b >= 0;
    ensures \result > 0;
*/
int power(int a, int b){
  int c = 1;
  for(int i = 0 ; i < b; i++){
    c = c * a;
  }
  return c;
}

int main() {
  int x = __VERIFIER_nondet_int();
  int y = -1;
  while(y < 0){
    y = __VERIFIER_nondet_int();
  }
  int z = power(x, y);
  if(z <= 0) ERROR: return 1;
  
  return 0;
}
