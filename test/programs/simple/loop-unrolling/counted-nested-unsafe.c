// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two nested loops that the heuristic both counts, so nothing is left of the nest.
//
// The heuristic counts both loops, so both are unrolled.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  while (i < 3) {
    int j = 0;
    while (j < 2) {
      s = s + 1;
      j = j + 1;
    }
    i = i + 1;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 3 && s == 6) {
    reach_error();
  }
  return 0;
}
