// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct point {
  int x;
  int y;
};

int g;
int shadowed;

int square(int v) { return v * v; }

int counter(void) {
  static int calls = 0;
  calls++;
  return calls;
}

int main(void) {
  int i = 0;
  int shadowed = 1;
  struct point p = {1, 2};
  for (int i = 0; i < 3; i++) {
    g = square(i);
  }
  {
    int i = 7;
    p.y = i;
  }
  return counter() + i + shadowed;
}

int choose(int b) {
  int r = b ? square(b) : 0;
  return r;
}
