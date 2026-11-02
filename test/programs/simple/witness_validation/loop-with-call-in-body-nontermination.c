// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The loop does not terminate for x > 0, since the call of inc() undoes the decrement of x.

extern int __VERIFIER_nondet_int(void);

int x;

void inc(void) { x = x + 1; }

int main() {
  x = __VERIFIER_nondet_int();
  while (x > 0) {
    x = x - 1;
    inc();
  }
  return 0;
}
