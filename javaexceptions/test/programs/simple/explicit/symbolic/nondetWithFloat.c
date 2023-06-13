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
  float b = 2.3;

  if (a < 0) {
    a = a + 1;
    if (a < 0) {
      a = -a;
    }
  }

  b += a;

  if (b < 1) {
ERROR:
    return -1;
  }
}
