// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The second iteration writes to the array of the first one, whose lifetime ended with the first
// copy of the body.
//
// The heuristic counts this loop, so it is unrolled.

int main(void) {
  int i = 0;
  int *p;
  while (i < 2) {
    int a[10];
    if (i == 0) {
      p = a;
    } else {
      p[0] = 1;
    }
    i = i + 1;
  }
  return 0;
}
