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
  unsigned char a;
  int b;
};

int main() {
  struct P p;
  p.a = 0;
  p.b = 0;
  __VERIFIER_nondet_memory(&p, 5);
  if (p.b > 255) {
    // this branch can not be entered.
    // The 5 havocked bytes cover p.a, the 3 padding bytes between p.a and p.b,
    // and then only the first byte of p.b. So p.b's upper 3 bytes stay 0
    // and p.b can never exceed 255.
    reach_error();
  }
}
