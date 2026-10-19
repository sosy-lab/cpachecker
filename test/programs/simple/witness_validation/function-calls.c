// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int check(int b) {
  int r = b + 1;
  return r;
}

int main() {
  int i = 0;
  while (i < 5) {
    i++;
    check(i != 3);
  }
  return 0;
}
