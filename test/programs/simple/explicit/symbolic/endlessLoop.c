// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();;
  int b = 5;

  while (1) {
    a = a + __VERIFIER_nondet_int();

    if (a > 0) {
      b = 10;
    }

    if (b == 20) {
ERROR:
      return -1;
    }
  }
}
