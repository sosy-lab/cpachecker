// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The counter moves away from the bound of an inequality, so only the wrap-around of the
// unsigned type ever brings it there.
//
// The heuristic gives up on this loop because the counter never lands on its bound, so it is kept.

extern void reach_error(void);

int main(void) {
  unsigned char i = 0;
  int s = 0;
  while (i != 5) {
    s = s + 1;
    i = i - 1;
  }
  if (!(i == 5 && s == 251)) {
    reach_error();
  }
  return 0;
}
