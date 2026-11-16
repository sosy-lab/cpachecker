// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}
extern unsigned int __VERIFIER_nondet_uint(void);

typedef struct {
  unsigned int a : 1;
  char b[13];
  unsigned int c : 2;
  unsigned int d : 3;
} s_t;

int main() {
  s_t s;
  s.c = 3;
  // Any value whose lowest 3 bits are 101 reaches the error.
  s.d = __VERIFIER_nondet_uint();
  if (s.d == 5 && s.c == 3) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
