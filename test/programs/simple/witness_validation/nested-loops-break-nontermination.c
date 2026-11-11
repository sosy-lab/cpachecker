// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The outer loop does not terminate, since the inner loop can be left by the break after
// decrementing x, and then x is 0.

extern int __VERIFIER_nondet_int(void);

int main() {
  int n = __VERIFIER_nondet_int();
  int x = 0;
  while (n > 0) {
    x = 1;
    while (x < 10) {
      x = x - 1;
      if (__VERIFIER_nondet_int()) {
        break;
      }
      x = x + 2;
    }
    n = n - x;
  }
  return 0;
}
