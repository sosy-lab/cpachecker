// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error(){}
void __VERIFIER_assert(int cond) {
  if (!cond) {
    reach_error();
  }
}

extern int __VERIFIER_nondet_int(void);

int main() {
  int x = __VERIFIER_nondet_int();
  int i = 0;
  if (x < 1000000) {
    while (x<1000000) {
      i = 0;
      while (i<5000 + 1) {
        x+=1;
        i+=1;
      }
      i=0;
      while (i<5000) {
        x-=1;
        i+=1
      }
    }
    __VERIFIER_assert(x == 1000000);
  }
  return 0;
}
