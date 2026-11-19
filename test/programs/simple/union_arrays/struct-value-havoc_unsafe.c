// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

struct pair {
  unsigned int a;
  unsigned int c;
};

union u {
  struct pair p;
  unsigned char b[8];
};

int main() {
  union u x;
  x.b[0] = 5;
  struct pair t = {7, 9};
  // b[0] is now a byte of t.a (7 or 0, depending on the byte order), but never 5.
  // CPAchecker treats assignments from parts of a struct value as non_det()s
  x.p = t;
  if (x.b[0] != 5) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
