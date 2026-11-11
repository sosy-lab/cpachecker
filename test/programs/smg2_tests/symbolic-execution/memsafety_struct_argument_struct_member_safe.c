// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A struct argument is copied completely from a struct member.

struct inner {
  int a;
  int b;
};

struct s {
  int x;
  struct inner in;
} g;

int check(struct inner n) { return n.b; }

int main(void) {
  g.in.b = 5;
  if (check(g.in) != 5) {
    int *p = 0;
    *p = 1;
  }
  return 0;
}
