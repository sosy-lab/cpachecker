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

extern struct s nondet_struct_s();

void main() {
  struct s s1 = {1};
  struct s s2 = {2};
  s1 = nondet_struct_s();
  if (s2.x != 2) {
ERROR:
    return;
  }
}
