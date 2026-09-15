// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

int main() {
  int a = __VERIFIER_nondet_int();
  if (a > 0) {
    if (a < 1000) {
      int b = a + a;
      return b;
    }
  }
  return 0;
}
