// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

int main() {
  int a;
  while (__VERIFIER_nondet_int()) {
    a = 19;
    //@ assert a == 19;
    a++;
    if (a == 20) {
      a = 10;
    }
  }
  if (a < 10) ERROR: return 1;
  return 0;
}
