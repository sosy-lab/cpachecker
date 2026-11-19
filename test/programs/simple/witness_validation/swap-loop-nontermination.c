// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

int main() {
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();
  while (x != y) {
    int tmp = x;
    x = y;
    y = tmp;
    if (__VERIFIER_nondet_int()) {
      break;
    }
  }
  return 0;
}
