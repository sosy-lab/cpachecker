// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error();

// The address of the parameter is taken in every frame, so every frame needs its own SSA copy of i.
int f(int i) {
  int *p = &i;
  if (*p == 2) {
ERROR:
    reach_error();
    return 0;
  }
  if (*p < 2) {
    return f(*p + 1);
  }
  return 1;
}

int main() {
  return f(0);
}
