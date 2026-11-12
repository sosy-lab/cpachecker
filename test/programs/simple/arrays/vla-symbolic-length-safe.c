// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint(void);

int main() {
  unsigned int n = __VERIFIER_nondet_uint();
  if (n < 2 || n > 10) {
    return 0;
  }
  int a[n];
  a[0] = 1;
  a[1] = 2;
  return a[0];
}
