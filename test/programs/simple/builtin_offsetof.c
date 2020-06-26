// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct s1 {
  int i1;
  struct s2 {
    double d1;
    int i2;
  } s2;
};

int offset = __builtin_offsetof(struct s1, s2.i2);

int main() {
  if (offset != 12) {
ERROR:
    return 1;
  }
  return 0;
}
