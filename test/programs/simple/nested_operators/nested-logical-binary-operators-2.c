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

int main() {
  int a = 4, b = 2, d = 0;
  f(d = a && b, 0);
  f(d = a || b, 0);
  return 0;
}
