// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A loop that is left when its counter lands exactly on the bound of an inequality.
//
// The heuristic counts this loop, so it is unrolled.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  while (i != 6) {
    s = s + 1;
    i = i + 2;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 6 && s == 3) {
    reach_error();
  }
  return 0;
}
