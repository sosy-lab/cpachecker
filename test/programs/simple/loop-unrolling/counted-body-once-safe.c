// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A loop that runs its body exactly once.
//
// The heuristic counts this loop, so it is unrolled.

extern void reach_error(void);

int main(void) {
  int i = 2;
  int s = 0;
  while (i < 3) {
    s = s + 1;
    i = i + 1;
  }
  if (!(i == 3 && s == 1)) {
    reach_error();
  }
  return 0;
}
