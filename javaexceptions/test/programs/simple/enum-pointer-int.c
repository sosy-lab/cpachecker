// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

enum B {
  b = 1,
  c = 2
};

void func(int *arg) {
  *arg = c;
};

int main(void) {
  enum B var = b;

  func((int*)&var);
  if (var == 2) {
  ERROR:
    return 1;
  }
  return 0;
}
