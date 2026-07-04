// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 5;
  int y = 1;

  while (x > 0) {
    x = 3 * x - 1;
    y = y * 2;
  }

  if(y > 32){
    ERROR: return 0;
  }

  return 1;
}