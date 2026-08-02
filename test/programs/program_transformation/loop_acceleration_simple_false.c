// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 5;
  int y = 3;
  int i = 0;

  while (i < 10) {
    x = -1 * x + 3 * i + 1;
    y = y + 2;
    i = i + 1;
  }

  if(y == 23){
    ERROR: return 0;
  }

  return 1;
}