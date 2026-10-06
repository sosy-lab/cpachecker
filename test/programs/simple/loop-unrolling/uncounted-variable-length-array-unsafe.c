// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The loop declares a variable length array, whose length lives in its type and is not
// renamed along with the variables of a copy.
//
// The heuristic gives up on this loop because it declares a variable without a constant size, so it is kept.

extern void reach_error(void);

int main(void) {
  int i = 0;
  int s = 0;
  while (i < 3) {
    int n = i + 2;
    int a[n];
    a[0] = 1;
    s = s + a[0];
    i = i + 1;
  }
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 3 && s == 3) {
    reach_error();
  }
  return 0;
}
