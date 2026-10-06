// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The counter is also written through a pointer, which an edge that writes the variable
// does not show. The value analysis cannot prove the state after these writes, so the
// safe variant only checks a variable that the loop does not touch.
//
// The heuristic gives up on this loop because the address of the counter is taken, so it is kept.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  int x = 7;
  int *p = &i;
  while (i < 3) {
    s = s + 1;
    i = i + 1;
    *p = *p + 1;
  }
  if (!(x == 7)) {
    reach_error();
  }
  return 0;
}
