// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct s {
  int x;
  char c[1];
};
void main() {
  struct s s = { 0, 0 };
  if (s.x != 0) {
ERROR:
    return;
  }
}
