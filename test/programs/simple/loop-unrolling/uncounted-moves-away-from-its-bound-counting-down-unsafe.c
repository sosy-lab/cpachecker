// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The counter moves away from the bound of a loop that counts down, so only the
// wrap-around of the unsigned type ends the loop.
//
// The heuristic gives up on this loop because the counter never reaches its bound, so it is kept.

extern void reach_error(void);

int main(void) {
  unsigned char i = 10;
  int s = 0;
  while (i > 4) {
    s = s + 1;
    i = i + 1;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 0 && s == 246) {
    reach_error();
  }
  return 0;
}
