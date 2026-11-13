// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

// The array makes the struct live in the heap model. d starts at bit 114,
// which is not a byte boundary.
typedef struct {
  unsigned int a : 1;
  char b[13];
  unsigned int c : 2;
  unsigned int d : 3;
} s_t;

int main() {
  s_t s = {.a = 1, .c = 2, .d = 5};
  s_t t = s;
  // 10 is truncated to the 3 bits of d.
  s.d = 10;
  if (t.d != 5 || s.d != 2 || s.c != 2 || s.a != 1) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
