// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

union words {
  unsigned int w[2];
  struct {
    unsigned short lo;
    unsigned short hi;
  } s;
};

union bytes {
  unsigned char b[8];
  unsigned int v[2];
};

int main() {
  union words x = {.w = {0x11111111u, 0x33333333u}};

  // s.hi covers only half of w[0], so the other half of w[0] and all of w[1] must be kept.
  x.s.hi = 0x2222;
  int halfKept = (x.w[0] == 0x22221111u || x.w[0] == 0x11112222u) && x.w[1] == 0x33333333u;


  union bytes y = {.b = {9, 9, 9, 9, 9, 9, 9, 9}};

  // v[1] covers b[4] to b[7], each of them gets its own byte of the value
  y.v[1] = 0x01020201u;
  int halfOverwritten =
      y.b[3] == 9 && y.b[4] == 0x01 && y.b[5] == 0x02 && y.b[6] == 0x02 && y.b[7] == 0x01;

  if (!halfKept || !halfOverwritten) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
