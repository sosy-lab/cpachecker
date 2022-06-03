// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
extern __nondet_int();

int main() {
  int x = __nondet_int();
//  int x = 5;
  if (x == 0) {
      x++;
  }
  if (x==0) {
    return 0;
  }
//  return 0;

  ERROR:
  return (-1);
}

