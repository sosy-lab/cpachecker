// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The address of an array element whose index is the counter is not the address of the
// counter. The heuristic looks for taken addresses in the whole function, so it does not
// matter that this one comes after the loop. The value analysis does not track the counter
// once its address might be taken, so the safe variant only checks a variable that the
// loop does not touch.
//
// The heuristic counts this loop, so it is unrolled.

extern void reach_error(void);

int main(void) {
  int a[8];
  int i = 0;
  int s = 0;
  int x = 7;
  while (i < 3) {
    s = s + 1;
    i = i + 1;
  }
  int *p = &a[i];
  // Only reached with these values if the loop runs as often as in the program.
  if (i == 3 && s == 3) {
    reach_error();
  }
  return 0;
}
