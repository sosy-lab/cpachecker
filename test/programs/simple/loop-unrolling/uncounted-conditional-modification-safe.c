// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Which iteration leaves the loop depends on the values of other variables.
//
// The heuristic gives up on this loop because the counter is not written on every path through the body, so it is kept.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int x = 1;
  int s = 0;
  while (i < 3) {
    s = s + 1;
    if (x > 0) {
      i = i + 1;
    }
  }
  if (!(i == 3 && s == 3)) {
    reach_error();
  }
  return 0;
}
