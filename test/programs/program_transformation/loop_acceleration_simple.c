// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 2;
  int y = 1;
  int z = 3;

  while (x != 0) {
    x = -1 * x + 2 * y - 3 * z;
    y = 4 * z;
    z = z;
  }

  if(y > 32){
    ERROR: return 0;
  }

  return 1;
}