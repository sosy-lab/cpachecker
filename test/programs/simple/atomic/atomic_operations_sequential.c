// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error(void);
extern void abort(void);

_Atomic int x = 0;
int y = 0;
int z = 0;

int main() {
  x += 1;
  y = ++x;
  z = x++;

  if (y != 2 || z != 2 || x != 3) {
    ERROR: {reach_error();abort();}
  }

  return 0;
}
