
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

/*@ ensures \result == a * a;*/
int square (int a){
  return a * a;
}

int main() {
  int x = __VERIFIER_nondet_int();
  int y = square(x);
  if(y != x*x) ERROR: return 1;
  
  return 0;
}
