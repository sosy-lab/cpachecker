// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Structures that are passed to and returned from functions by value keep their members.
extern int __VERIFIER_nondet_int();

struct inner {
  int x;
  int y;
};

struct pair {
  int a;
  struct inner in;
};

int sum(struct pair p) { return p.a + p.in.x + p.in.y; }

struct pair make(int a) {
  struct pair p;
  p.a = a;
  p.in.x = a + 1;
  p.in.y = a + 2;
  return p;
}

int main() {
  int a = __VERIFIER_nondet_int();
  if (a < 0 || a > 100) {
    return 0;
  }
  struct pair p = make(a);
  if (sum(p) != 3 * a + 3) {
    ERROR: goto ERROR;
  }
  return 0;
}
