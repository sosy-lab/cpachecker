// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort();
void reach_error(){}
extern void *malloc(unsigned long);

// Every execution of the allocation returns the address of a new block, so the two blocks that the
// loop allocates never have the same address and the error is unreachable.
int main() {
  int *a = 0;
  int *b = 0;
  for (int i = 0; i < 2; i++) {
    int *p = malloc(sizeof(int));
    if (!p) {
      return 0;
    }
    if (a == 0) {
      a = p;
    } else {
      b = p;
    }
  }
  if (a == b) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
