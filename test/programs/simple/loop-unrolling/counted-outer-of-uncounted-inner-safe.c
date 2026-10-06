// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// An outer loop that is counted around an inner loop that is not, because its bound is a
// variable. Every copy of the outer body keeps its own copy of the inner loop.
//
// The heuristic counts the outer loop only, so the inner loop stays a loop in every copy.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int j = 0;
  int s = 0;
  while (i < 3) {
    j = 0;
    while (j < i) {
      s = s + 1;
      j = j + 1;
    }
    i = i + 1;
  }
  if (!(i == 3 && s == 3)) {
    reach_error();
  }
  return 0;
}
