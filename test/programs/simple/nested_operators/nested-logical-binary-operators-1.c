// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void f(int value, int expect) {
  if (value != expect) {
  ERROR:
    goto ERROR;
  }
}

int getDouble(int value) { return value * 2; }

int logicalOr(int op1, int op2) { return op1 || op2; }

int logicalAnd(int op1, int op2) { return op1 && op2; }

int main() {
  int c = 4, d = 7, e = 2;
  f(d = c && e, 1);
  f(d = c || e, 1);

  int h = 8, g = 3;
  f(d = h && e || g && c, 1);

  f(d = c && d && e && h && g, 1);

  f(d = getDouble(c) || getDouble(e), 1);

  if (!(d = (getDouble(h) || g))) {
    f(d, 0);
  }

  f(d = logicalOr(c, d) && e || !logicalAnd(h, g), 1);

  int b, a;

  !(d = !logicalOr(c, d) || (b = e && 0)) ? f(d, 0) : f(d, 1);

  f(d = (a = ((a = c && e) || (b = h && g))), 1);

  return 0;
}
