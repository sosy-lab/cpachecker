// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

int main() {
  int a = 0;
  while (__VERIFIER_nondet_int()) {
    do {
      a++;
      //@ assert a <= 20;
    } while (a != 20);
  }
  if (a < 10) ERROR: return 1;
  return 0;
}
