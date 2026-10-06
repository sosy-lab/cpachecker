// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A while loop that checks its condition before each run of its body.
//
// The heuristic counts this loop, so it is unrolled.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  while (i < 3) {
    s = s + i;
    i = i + 1;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 3 && s == 3) {
    reach_error();
  }
  return 0;
}
