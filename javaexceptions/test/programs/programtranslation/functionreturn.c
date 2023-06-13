// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int foo(int x) {
  if (x) {
    return (x);
  }
  return 1;
}

int main() {
  int y = 0;
  y = foo(y);
  if (y) {
    ERROR:
    goto ERROR;
  } else {
    return 0;
  }
  return -1;
}
