// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_int(void);

int main(){
  int x = -1;
  int y = -1;
  unsigned int p = 0;
  while(x < 0){
    x = __VERIFIER_nondet_int();
  }
  while (y < 0) {
    y = __VERIFIER_nondet_int();
  }

  for(int i = 0; i < x; i++){
    int y = p + x;
    //@ assert y == i * x;
    p = y;

  } 
  return 0;
}
