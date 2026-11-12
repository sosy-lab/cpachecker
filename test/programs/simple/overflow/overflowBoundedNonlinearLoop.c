// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// No overflow, but proving it needs to keep the bound on the input a
// through the non-linear arithmetic in the loop.
extern unsigned short __VERIFIER_nondet_ushort(void);

int main() {
  unsigned short a = __VERIFIER_nondet_ushort();
  if (a > 1000) {
    return 0;
  }
  int x = 0;
  for (int i = 0; i < 10; i++) {
    x = x + a * a;
  }
  return x;
}
