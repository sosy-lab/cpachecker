// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stddef.h>

extern void __VERIFIER_error();

int main() {
  int b = 4, d = 2, n = 3, i = 9;
  int *c = &i;
  int *r = &d;
  int **a = &c;
  int **pp = &c;
  int ***ppp;
  if (ppp){
   a = &r;
   ppp = &a;
   }
  else {
    ppp = &pp;
  }
  **ppp = &n;
  if ((c != &n) || (r != &n)){
    ERROR:
    __VERIFIER_error();
  }
  return 0;
}