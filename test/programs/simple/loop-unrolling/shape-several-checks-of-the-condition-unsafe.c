// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A loop that reaches its condition twice per iteration, so that the condition has to
// stay a condition in every copy.
//
// The heuristic gives up on this loop because its counter is written more than once per iteration, so it is kept.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  goto start;
  start:
    s = s + 1;
  check:
    if (i >= 4) {
      goto end;
    }
    i = i + 1;
    if (i == 2) {
      goto check;
    }
    goto start;
  end:
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 4 && s == 4) {
    reach_error();
  }
  return 0;
}
