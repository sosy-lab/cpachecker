// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A loop that is entered in the middle of its body, so its exit is not where a run of
// the body ends. The last unrolling must stop at the exit instead of running the rest of
// the body once more.
//
// The heuristic counts this loop, so it is unrolled.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  goto middle;
  body:
    s = s + i;
  middle:
    i = i + 1;
    if (i < 3) {
      goto body;
    }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 3 && s == 3) {
    reach_error();
  }
  return 0;
}
