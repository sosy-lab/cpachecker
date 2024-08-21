// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern __VERIFIER_nondet_int();

int main() {
  int a;
  int b;

  a = __VERIFIER_nondet_int();
  b = __VERIFIER_nondet_int();

  if (a == b) {
    goto ERROR;
  } else {
    a = b;
  }

  if (a == b) {
    return 0;
  } else {
ERROR:
    return -1;
  }
}
