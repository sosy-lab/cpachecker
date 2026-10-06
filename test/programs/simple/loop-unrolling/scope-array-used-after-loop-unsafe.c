// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// After the loop, the pointer still points to the array of the last copy of the body, whose
// lifetime has ended.
//
// The heuristic counts this loop, so it is unrolled.

int main(void) {
  int i = 0;
  int *p;
  while (i < 10) {
    int a[10];
    p = a;
    p[0] = 1;
    i = i + 1;
  }
  p[0] = 2;
  return 0;
}
