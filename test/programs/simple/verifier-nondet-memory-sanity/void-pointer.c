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
  int x = 0;
  void *p = &x;
  // Make sure that void pointers are correctly handled
  __VERIFIER_nondet_memory(p, sizeof(x));
  if (x == 42) {
    // branch may be entered
    reach_error();
  }
}
