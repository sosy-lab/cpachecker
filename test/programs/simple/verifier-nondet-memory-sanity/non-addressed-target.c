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
  int a[1] = {0};
  __VERIFIER_nondet_memory(a, sizeof(a));
  if (a[0] == 1) {
    reach_error();
  }
  int y = a[0] + 1; // signed overflow for a[0] == INT_MAX
  return y != 0;
}
