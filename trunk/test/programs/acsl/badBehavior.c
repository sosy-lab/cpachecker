// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

int main() {
  int x = 10;
  int y = __VERIFIER_nondet_int();
  /*@ for notZero: ensures x == 20; */
  if (y) {
    x = 20;
  }
  return 0;
}
