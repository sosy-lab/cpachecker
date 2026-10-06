// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The counter is written twice per iteration.
//
// The heuristic gives up on this loop because the counter is written by more than one edge, so it is kept.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  while (i < 8) {
    s = s + 1;
    i = i + 1;
    i = i + 2;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 9 && s == 3) {
    reach_error();
  }
  return 0;
}
