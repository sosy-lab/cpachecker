// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The counter moves away from the bound, but the loop is left before it moves at all.
//
// The heuristic counts this loop, so it is unrolled.

extern void reach_error(void);

int main(void) {
  int i = 5;
  int s = 0;
  while (i < 3) {
    s = s + 1;
    i = i - 1;
  }
  if (!(i == 5 && s == 0)) {
    reach_error();
  }
  return 0;
}
