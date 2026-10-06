// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The outer counter is written in a nested loop, so how often that one runs would have
// to be known as well.
//
// The heuristic counts the inner loop, but gives up on the outer one because its counter is then written several times per iteration.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int j = 0;
  int s = 0;
  while (i < 6) {
    s = s + 1;
    j = 0;
    while (j < 2) {
      i = i + 1;
      j = j + 1;
    }
  }
  if (!(i == 6 && s == 3)) {
    reach_error();
  }
  return 0;
}
