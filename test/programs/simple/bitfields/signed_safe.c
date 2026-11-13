// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

typedef struct {
  char arr[1];
  signed int a : 1;
  signed int b : 3;
} s_t;

int main() {
  s_t s;
  s.a = 0;
  s.b = -1;
  int minusOne = s.b;
  // 5 is 101 in 3 bits, which is -3 as a signed value.
  s.b = 5;
  if (s.a != 0 || minusOne != -1 || s.b != -3) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
