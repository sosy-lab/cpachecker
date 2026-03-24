// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error();

void f(int x) {
  if (x > 1000000) {
    return;
  }
  if (x % 2) {
    reach_error();
  }
  x += 2;
  f(x);
}

int main() {
  f(0);
  return 0;
}
