// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main() {
  int x = __VERIFIER_nondet_int();
  if (x <= 0) {
    x = 100;
  }
  if (x > 100) {
    x = 100;
  }
  int y = 10;
  if (x + y == 10) {
    y = y - x;
  } else {
    y = y + x;
  }
  if (y != 10) {
    goto ERROR;
  }
  return 0;
ERROR:
  return 1;
}
