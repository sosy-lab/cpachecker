// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main() {

  int x = 0;
  x++;
  x++;
  if (x == 2) {
    x++;
    x++;
  } else {
    x++;
    x++;
  }
  x--;
  x--;

  if (x == 2) {
    goto ERROR;
  }
  return 0;
ERROR:
  return 1;
}
