// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

void reach_error() {}
extern void __VERIFIER_nondet_memory(void *ptr, size_t size);

struct S {
  int *p;
  unsigned char b[4];
};

int main() {
  int x = 1;
  struct S s = {&x, {0, 0, 0, 7}};
  // only the first 3 of the 4 bytes of s.b become nondeterministic
  __VERIFIER_nondet_memory(&s.b[0], 3);
  // s.p is not set to nondet, so still points to x
  *s.p = 2;
  if (s.b[3] != 7 || x != 2) {
    // s.b[3] is 7 and x is 2, so this branch is not entered
    reach_error();
  }
}
