// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Every iteration writes to the array of its own copy of the body, which is still alive.
//
// The heuristic counts this loop, so it is unrolled.

int main(void) {
  int i = 0;
  int *p;
  while (i < 2) {
    int a[10];
    p = a;
    if (i != 0) {
      p[0] = 1;
    }
    i = i + 1;
  }
  return 0;
}
