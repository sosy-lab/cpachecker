// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The callee modifies a local variable of its caller through a pointer.
// Restoring the values of a caller after a function call must not undo this.
extern void reach_error(void);

void increment(int *p) { (*p)++; }

int main(void) {
  int x = 0;
  increment(&x);
  increment(&x);
  if (x != 2) {
    reach_error();
  }
  return 0;
}
