// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The counter is not initialized, so it has no known start value.
//
// The heuristic gives up on this loop because the counter has no known start value, so it is kept.

extern void reach_error(void);

int main(void) {
  int i;
  int x = 7;
  while (i < 3) {
    i = i + 1;
  }
  if (!(x == 7)) {
    reach_error();
  }
  return 0;
}
