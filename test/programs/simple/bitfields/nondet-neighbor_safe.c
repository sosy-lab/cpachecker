// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

// y (bits 12 to 15) and w (bits 19 to 23) are stored in the same unsigned int.
struct s {
  unsigned char tag;
  unsigned int x : 4;
  unsigned int y : 4;
  unsigned int z : 3;
  unsigned int w : 5;
};
struct t {
  unsigned char tag;
  unsigned char xy;
};
union u {
  struct s s;
  struct t t;
  unsigned char raw[4];
};

extern struct t get_t(void);

int main() {
  union u v;
  v.s.w = 9;
  // t covers bytes 0 and 1, so tag, x, and y become nondet, but w must keep its value.
  v.t = get_t();
  if (v.s.w != 9) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
