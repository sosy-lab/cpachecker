// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The program has no loop, no recursion and no backward goto, so every execution ends.
extern int __VERIFIER_nondet_int(void);

int twice(int x) { return x + x; }

int main(void) {
  int x = __VERIFIER_nondet_int();
  if (x > 0) {
    return twice(x);
  }
  return 0;
}
