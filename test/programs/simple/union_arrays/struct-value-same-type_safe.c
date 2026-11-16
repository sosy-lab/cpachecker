// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}
extern unsigned int __VERIFIER_nondet_uint(void);

struct pair {
  unsigned int a;
  unsigned int c;
};

union u {
  struct pair p[2];
  unsigned int w[4];
};

int main() {
  union u x = {.w = {1, 2, 3, 4}};
  struct pair t = {__VERIFIER_nondet_uint(), __VERIFIER_nondet_uint()};
  // p[1] gets the whole struct value. w[0] and w[1] should be retained
  // w[2] and w[3] should get the same value as p[1]
  x.p[1] = t;
  if (x.p[1].a != t.a || x.p[1].c != t.c || x.w[2] != t.a || x.w[3] != t.c || x.w[0] != 1
      || x.w[1] != 2) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
