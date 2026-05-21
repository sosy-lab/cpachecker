// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

/*@
  requires \valid(p);
  assigns *p;
  ensures *p == \old(*p) + 1;
*/
void pointer_increment(int *p){
  int v = *p +1;
  p = &v;
}

int main(){
  return 0;
}
