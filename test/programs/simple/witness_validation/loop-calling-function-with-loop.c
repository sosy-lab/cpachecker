// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The loop terminates, since count(x) returns x for x > 0 and the loop in count() terminates.

extern int __VERIFIER_nondet_int(void);

int count(int n) {
  int i = 0;
  while (i < n) {
    i = i + 1;
  }
  return i;
}

int main() {
  int x = __VERIFIER_nondet_int();
  while (x > 0) {
    x = x - count(x);
  }
  return 0;
}
