// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {

  int x = 0;
  int y = 4;

  if (y < 5) {
    if (y == 4) {
      x++;
      x++;
    }
    x++;
    y++;
    y++;
    y++;
  }

  if (x == 3) {
  ERROR:
    return 1;
  }
  return 0;
}
