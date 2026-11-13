// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The unknown function may change x, so the execution cannot prove that the loop terminates.
extern void set(int *x);

int main(void) {
  int x = 0;
  set(&x);
  while (x != 0) {
  }
  return 0;
}
