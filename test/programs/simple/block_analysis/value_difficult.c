// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main () {

  int x = __VERIFER_nondet_int();
  for (int i = 0; i < 1050; i++) {
     x++;
  }
  int j = 0;
  for (; j < 5; j++) {}
  if (j != 5) reach_error();

}
