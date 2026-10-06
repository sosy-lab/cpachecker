// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Inside the same copy of the body, the array of a nested block is used after that block ended.
//
// The heuristic counts this loop, so it is unrolled.

int main(void) {
  int i = 0;
  int *p;
  while (i < 3) {
    {
      int b[2];
      p = b;
    }
    p[0] = 1;
    i = i + 1;
  }
  return 0;
}
