// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Every copy of the loop body declares its own arrays, and each of them ends its lifetime at the
// end of its block in that copy: the one of the body before the loop starts over, and the one of
// the nested block right after that block. Nothing uses them afterwards.
//
// The heuristic counts this loop, so it is unrolled.

int main(void) {
  int i = 0;
  int *p;
  while (i < 3) {
    int a[2];
    p = a;
    p[0] = 1;
    {
      int b[2];
      p = b;
      p[0] = 1;
    }
    i = i + 1;
  }
  return 0;
}
