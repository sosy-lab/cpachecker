// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A do-while loop, whose counter is already changed when it checks its condition.
//
// The heuristic counts this loop, so it is unrolled.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  do {
    s = s + i;
    i = i + 1;
  } while (i < 3);
  if (!(i == 3 && s == 3)) {
    reach_error();
  }
  return 0;
}
