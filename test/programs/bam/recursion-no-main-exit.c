// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The recursion makes BAM analyze the whole program inside of blocks, and the
// unconditional abort() keeps main's block from ever reaching main's exit node.
// The main reached set therefore contains nothing but its root state.

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *);
void reach_error() { __assert_fail("0", "recursion-no-main-exit.c", 15, "reach_error"); }

int f(int n) {
  if (n <= 0) {
    return 0;
  }
  return f(n - 1) + 1;
}

int main(void) {
  if (f(3) != 3) {
    ERROR: {reach_error(); abort();}
  }
  abort();
}
