// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The initializer of one variable of the loop reads another one of the same copy.
//
// The heuristic counts this loop, so it is unrolled.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  while (i < 3) {
    int a = i + 1;
    int b = a + 1;
    s = s + b;
    i = i + 1;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 3 && s == 9) {
    reach_error();
  }
  return 0;
}
