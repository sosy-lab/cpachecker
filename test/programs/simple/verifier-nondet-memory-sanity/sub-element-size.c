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

int main() {
  unsigned int x = 0;
  // Only make the least significant byte a nondeterministic value; all other bytes stay zero.
  // This means x is _at most_ 255, but not larger.
  __VERIFIER_nondet_memory(&x, 1);
  if (x > 255) {
    reach_error();
  }
}
