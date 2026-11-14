// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

// y (bits 6 to 9) crosses a byte boundary, x, y, and z share bytes.
typedef struct {
  unsigned int x : 6;
  unsigned int y : 4;
  unsigned int z : 6;
  char arr[2];
} s_t;

int main() {
  s_t s;
  s.x = 63;
  s.y = 10;
  s.z = 33;
  // Writing y must keep x and z, which share bytes with y.
  s.y = 5;
  if (s.x != 63 || s.y != 5 || s.z != 33) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
