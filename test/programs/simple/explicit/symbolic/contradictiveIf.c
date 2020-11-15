// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();
  int b = __VERIFIER_nondet_int();
  int * c = &a;

  if (c != c) {
    goto ERROR;
  }

  if (a > a) {
    goto ERROR;
  }
  
  a = __VERIFIER_nondet_int();  

  if (a > b) {
    if (a < b) {
ERROR:
      return -1;
    }
  }

  return 0;
}
