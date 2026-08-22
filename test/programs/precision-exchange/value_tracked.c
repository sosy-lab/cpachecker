// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error(void);

// Safe: x and y have concrete values, so the guard is infeasible. Proving this
// requires the value analysis to track the memory locations of x and y, so a
// non-trivial precision is refined and exported.
int main() {
  int x = 5;
  int y = 10;

  if (x + y != 15) {
    reach_error();
  }

  return 0;
}
