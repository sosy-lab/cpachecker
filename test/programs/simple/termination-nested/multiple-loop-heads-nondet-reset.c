// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

// The jump to B makes one loop with the two heads A and B. A decreases x and sets y to any value,
// and B decreases y.
int main() {
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();
  if (__VERIFIER_nondet_int()) goto B;
A:
  if (x <= 0) return 0;
  x--;
  y = __VERIFIER_nondet_int();
  if (__VERIFIER_nondet_int()) goto A;
B:
  if (y <= 0) goto C;
  y--;
  goto B;
C:
  goto A;
}
