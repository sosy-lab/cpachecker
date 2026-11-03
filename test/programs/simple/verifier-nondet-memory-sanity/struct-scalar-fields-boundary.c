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
  int a;
  int b;
};

int main() {
  struct S s;
  s.a = 0;
  s.b = 3;
  __VERIFIER_nondet_memory(&s, sizeof(s.a));
  if (s.b != 3) {
    reach_error();
  }
}
