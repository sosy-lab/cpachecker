// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 0;
  int y = 5;
  while (x != y) {
    x++;
  }
  if (x == y)
    goto ERROR;
  return 0;
ERROR:
  return 1;
}
