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

struct P {
  int a;
  char buf[8];
};

int main() {
  struct P p;
  p.a = 0;
  for (int i = 0; i < 8; i++) {
    p.buf[i] = 0;
  }
  p.buf[7] = 9;
  // havoks 6 bytes, up to (including) p.buf[1], but not after that.
  __VERIFIER_nondet_memory(&p, 6);
  if (p.buf[7] != 9) {
    // not reachable because p.buf[7] is still 9.
    reach_error();
  }
}
