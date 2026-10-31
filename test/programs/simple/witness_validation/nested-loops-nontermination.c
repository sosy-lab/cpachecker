// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The outer loop does not terminate, since the inner loop undoes the decrement of x.

int main() {
  int x = 10;
  int y = 0;
  while (x > 0) {
    x = x - 1;
    y = 0;
    while (y < 1) {
      y = y + 1;
      x = x + 1;
    }
  }
  return 0;
}
