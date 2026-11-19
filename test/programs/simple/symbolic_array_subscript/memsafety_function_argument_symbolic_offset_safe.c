// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Reading an argument at a symbolic offset may split the state.

extern int __VERIFIER_nondet_int(void);

int x;
int y;
int *ptrs[2] = {&x, &y};

void write(int *p) { *p = 1; }

int main(void) {
  int i = __VERIFIER_nondet_int();
  if (i < 0 || i >= 2) {
    return 0;
  }
  write(ptrs[i]);
  return 0;
}
