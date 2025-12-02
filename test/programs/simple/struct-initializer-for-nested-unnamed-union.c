// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() { }
typedef struct union_in_struct_s {
  union {
    char a; // 8 bit            00000000
    short b; // 16 bit  0000000100000000 (512)
  };
  int c;
} union_in_struct_t;

int main() {
  union_in_struct_t s = {.b = 512, .c = 10};
  union_in_struct_t t = {{.a = 0}, .c = 10};
  if (s.a != 1) {
    goto ERROR;
  }
  if (t.a != 0) {
    goto ERROR;
  }
  return 0;
ERROR:
    reach_error();
    return 1;
}
