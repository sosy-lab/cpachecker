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
  
  if (a < 0) {
    a++;
    if (a < 0) {
      a = -a;
    }
  }

  if (b < 0) {
    b++;
    if (b < 0) {
      b = -b;
    }
  }
  
  if (a == 0) {
    return a;

  } else {
    while (b != 0) {
      if (b < 0) {
        goto ERROR;
      }
      
      if (a > b) {
        a = a - b;

      } else {
        if (a < 0) {
          goto ERROR;
        }

        b = b - a;
      }
    }
    return a;
  }

ERROR:
  return -1;
}
