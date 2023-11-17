// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void f(int *p) {
  p[0] = 1;
}
int main() {
  int i = 0;
  f(&i);
  if (i == 1) {
ERROR:
    return 1;
  }
  return 0;
}
