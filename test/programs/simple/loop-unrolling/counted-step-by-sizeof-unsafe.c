// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A loop that steps by a constant expression, which the parser folds into a literal.
//
// The heuristic counts this loop, so it is unrolled.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  while (i < 12) {
    s = s + 1;
    i = i + sizeof(int);
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 12 && s == 3) {
    reach_error();
  }
  return 0;
}
