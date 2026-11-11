// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 0;
  while (x < 3) {
    int x = 1;
    break;
  }
  {
    int x = 2;
    {
      int y = x;
    }
  }
  return x;
}
