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
  int a[2] = {0, 0};
  size_t n = sizeof(a);
  // Check that sizeof-handling works even if not hardcoded as function parameter
  __VERIFIER_nondet_memory(&a[0], n);
  if (a[0] == 1 && a[1] == 2) {
    // this branch may be entered
    reach_error();
  }
  return 0;
}
