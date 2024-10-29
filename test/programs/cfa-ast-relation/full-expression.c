// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f() {
  int x = 1;
  int y = 1;
  x = 2;
  y = 2;
  while (x != 0 && y != 0) {
    x = x - 1;
    y = y - 1;
  }
  return x + y;
}

int g(int z, int w) { return z + w; }

int main() {
  f();
  g(1, 2);

  int i = 0;
  int j = 0;
  for (i == 0 || j < 0; j == 0 && i < 10; i < 5 && i != 0) {
    i++;
  }

  int q = 0;
  int s = 0;
  int p = s != q ? s == 1 : q == 2;
  int l = (0);
}