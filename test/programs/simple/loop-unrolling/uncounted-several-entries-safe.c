// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The loop is also entered in the middle of its body, so we would not know which copy
// the second entry belongs to.
//
// The heuristic gives up on this loop because it is entered at more than one node, so it is kept.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  if (s == 0) {
    goto body;
  }
  while (i < 3) {
  body:
    s = s + i;
    i = i + 1;
  }
  if (!(i == 3 && s == 3)) {
    reach_error();
  }
  return 0;
}
