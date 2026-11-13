// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

// b (bits 5 to 7) is stored in the same unsigned int as c.
typedef union {
  struct {
    unsigned int a : 5;
    unsigned int b : 3;
    unsigned int rest : 24;
  } s;
  unsigned int c;
  unsigned char raw[4];
} u_t;

int main() {
  u_t u;
  u.c = 0xFFu;
  int bBefore = u.s.b;
  // Writing b changes bits 5 to 7 of c from 111 to 010.
  u.s.b = 2;
  if (bBefore != 7 || u.c != 0x5Fu || u.s.a != 31) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
