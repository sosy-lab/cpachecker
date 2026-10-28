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
  struct P s;
  s.a = 0;
  s.b = 0;
  // The struct contains padding, so sizeof(s) is larger than the sum
  // of the sizes of its members (with ILP32: a at offset 0, three padding bytes,
  // b at offset 4, sizeof 8). The region covers the padding as well, and every
  // member behind the padding still has to become nondeterministic.
  __VERIFIER_nondet_memory(&s, sizeof(s));
  if (s.a == 1 && s.b == 2) {
    // reachable because both s.a and s.b are nondet
    reach_error();
  }
  return 0;
}
