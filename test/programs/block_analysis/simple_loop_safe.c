// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 0;
  while (x != 100) {
    x++;
  }
  if (x != 100)
    goto ERROR;
  return 0;
ERROR:
  return 1;
}
