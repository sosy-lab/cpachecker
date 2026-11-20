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
  unsigned char b[4];
  int i;
};

int main() {
  union U u;
  u.i = 0;
  __VERIFIER_nondet_memory(&u, sizeof(u));
  if (u.i == 42) {
    // branch may be entered
    reach_error();
  }
  return 0;
}
