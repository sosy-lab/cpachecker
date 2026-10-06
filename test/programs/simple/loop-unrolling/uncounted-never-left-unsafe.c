// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A loop that is never left, so there is no exit condition to count.
//
// The heuristic gives up on this loop because it is not left via one branch of a condition, so it is kept.

extern void reach_error(void);

int main(void) {
  unsigned char s = 0;
  int x = 7;
  while (1) {
    s = s + 1;
    // The body runs forever, so it reaches every value of the counter.
    if (s == 3) {
      reach_error();
    }
  }
  return 0;
}
