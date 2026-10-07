// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The program computes with integers only: it allocates nothing, dereferences nothing and
// takes no address, so it performs no memory operation that could be invalid.
extern int __VERIFIER_nondet_int(void);

int counter = 0;

int increment(int by) { return counter + by; }

int main(void) {
  int x = __VERIFIER_nondet_int();
  if (x > 0) {
    counter = increment(1);
  }
  return counter;
}
