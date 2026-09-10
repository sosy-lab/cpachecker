// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

extern void __VERIFIER_nondet_memory(void *ptr, size_t size);

struct S {
  unsigned char x;
  unsigned char y;
};

int main() {
  struct S s = {0, 0};
  __VERIFIER_nondet_memory(&s.x, sizeof(s.x));
  // s.y is always 0, so the loop is never entered
  // -> program terminates
  while (s.y != 0) {
  }
}
