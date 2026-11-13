// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint(void);

// The size n * sizeof(int) may wrap around in size_t, e.g., for n = 2^30 with ILP32
int main() {
  unsigned int n = __VERIFIER_nondet_uint();
  if (n < 2) {
    return 0;
  }
  int a[n];
  a[1] = 1;
  return 0;
}
