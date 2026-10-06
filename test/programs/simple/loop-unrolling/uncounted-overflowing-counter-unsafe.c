// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The counter leaves the range of its type before the loop is left, so the values that
// the heuristic computes are not the ones of the program.
//
// The heuristic gives up on this loop because the counter would overflow, so it is kept.

extern void reach_error(void);

int main(void) {
  signed char c = 100;
  int s = 0;
  while (c < 126) {
    s = s + 1;
    c = c + 10;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (c == 126 && s == 105) {
    reach_error();
  }
  return 0;
}
