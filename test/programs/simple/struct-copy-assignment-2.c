// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
struct s {
  int x;
  char c[1];
};
void main() {
  struct s s1 = {1,0};
  struct s s2;
  if (s1.x != 1 && s2.x != 0) {
ERROR:
    return;
  }
}
