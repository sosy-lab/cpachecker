// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

union u {
   int i1 ;
   int i2 ;
};

union u u[1] = {{ .i1 = 1 }};

extern void __VERIFIER_error() __attribute__ ((__noreturn__));
int main(void) {
  if (u[0].i1 == 1) {
ERROR:
    return 1;
  }
  return 0;
}
