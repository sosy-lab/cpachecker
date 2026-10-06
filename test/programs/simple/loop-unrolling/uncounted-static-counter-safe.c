// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A static counter keeps its value, so the second call of the function starts where the
// first one stopped and does not run the body at all.
//
// The heuristic gives up on this loop because its counter is static, so it is kept.

extern void reach_error(void);

int count(void) {
  static int i = 0;
  static int s = 0;
  while (i < 3) {
    s = s + 1;
    i = i + 1;
  }
  return s;
}

int main(void) {
  int first = count();
  int second = count();
  if (!(first == 3 && second == 3)) {
    reach_error();
  }
  return 0;
}
