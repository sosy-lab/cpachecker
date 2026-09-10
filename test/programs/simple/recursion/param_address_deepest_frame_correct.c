// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error();

// The address of the parameter is taken only in the deepest frame, so the outer frames keep using
// the SSA copy of i after the call.
void f(int i) {
  if (i < 2) {
    f(i + 1);
    if (i != 0 && i != 1) {
ERROR:
      reach_error();
    }
  } else {
    int *p = &i;
    *p = 42;
  }
}

int main() {
  f(0);
  return 0;
}
