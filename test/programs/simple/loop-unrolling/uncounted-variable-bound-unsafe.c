// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The loop compares its counter to a variable instead of a constant.
//
// The heuristic gives up on this loop because its bound is not a constant, so it is kept.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int n = 3;
  int s = 0;
  while (i < n) {
    s = s + 1;
    i = i + 1;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 3 && s == 3) {
    reach_error();
  }
  return 0;
}
