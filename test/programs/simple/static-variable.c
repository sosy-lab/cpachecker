// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void f() {
  static int s = 0;
  if (s == 0) {
    s = 1;
  } else if (s == 1) {
    s = 2;
  } else if (s == 2) {
ERROR:
    return;
  }
}

int main() {
  f();
  f();
  f();
  return 0;
}
