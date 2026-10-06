// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// With several ways out of the loop we cannot tell which one is the exit condition.
//
// The heuristic gives up on this loop because it has more than one outgoing edge, so it is kept.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  while (i < 3) {
    if (s > 100) {
      break;
    }
    s = s + i;
    i = i + 1;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 3 && s == 3) {
    reach_error();
  }
  return 0;
}
