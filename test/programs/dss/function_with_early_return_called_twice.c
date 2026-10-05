// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error(void);

int f(int a, int b) {
  int i = 0;
  if (a != b) {
    return 0;
  }
  if (i < a) {
    i++;
  }
  return i;
}

int main() {
  int x = f(1, 1);
  int y = f(1, 1);
  if (x != y) {
    reach_error();
  }
  return 0;
}
