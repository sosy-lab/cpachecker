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

union U {
  short a;
  unsigned int i;
};

int main() {
  union U u;
  u.i = 0x12340000;
  __VERIFIER_nondet_memory(&u.a, sizeof(u.a));
  if ((u.i & 0xFFFF0000) != 0x12340000) {
    // this branch is never entered, because the upper 4 bytes of u.i are not havoked.
    reach_error();
  }
  return 0;
}
