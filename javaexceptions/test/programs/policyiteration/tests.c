// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
  int x = 0;
  int y = 0;
  while (1) {
    x++;
    y++;
    x += y;
    y += x;

    if (x >= 10 || y >= 10) break;
  }

  assert(y >= 10);
}
