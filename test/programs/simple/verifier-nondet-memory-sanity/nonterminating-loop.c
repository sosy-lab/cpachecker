// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

extern void __VERIFIER_nondet_memory(void *ptr, size_t size);

int main() {
  unsigned char c = 0;
  __VERIFIER_nondet_memory(&c, sizeof(c));
  // c can be anything, so the loop may not terminate.
  while (c != 0) {
  }
}
