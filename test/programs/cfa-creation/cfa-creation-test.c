// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 0;
  int y = 0;
  if (x == y && x == 0 &&
      y == 0) {
    x++;
  }

  int t1 = 1;
  int t2 = 2;
  int t3 = 3;

  while (t1 == t2 ||
         t1 == t3 &&
         t2 ==
         t3) {
    t1++;
  }

  return y;
}