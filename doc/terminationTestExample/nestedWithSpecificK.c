// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
// k=16, 4 times inner head, 4 times outer head. 
// The outer loop failed on the 5th execution and will not be executed again.

int main() {
  unsigned int outer = 0;
  unsigned int outerMax = 4;

  while (outer < outerMax) {
    unsigned int inner = 0;
    unsigned int innerMax = 3;

    while (inner < innerMax) {
      inner = inner + 1;
    }

    outer = outer + 1;
  }

  return 0;
}
