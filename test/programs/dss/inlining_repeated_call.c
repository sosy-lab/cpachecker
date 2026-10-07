// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error(void);

int g;

void set(int v) {
  if (v < 0) {
    g = -v;
  } else {
    g = v;
  }
}

int main(void) {
  set(7);
  int a = g;
  set(-4);
  int b = g;
  if (a == 7 && b == 4) {
    reach_error();
  }
  return 0;
}
