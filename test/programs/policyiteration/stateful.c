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
  int i = 0;

  while (1) {
    if (i >=0 && i <= 5) {
      i -= 1;
    } else if (i <= -1) {
      i += 7;
    } else if (i == 6) {
      i = 100;
      break;
    } else {
      i = 1000;
      break;
    }
  }

  assert(i == 100);
}
