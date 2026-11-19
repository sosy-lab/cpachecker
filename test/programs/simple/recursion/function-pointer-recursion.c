// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// apply() is called again while it is on the call stack, via a function pointer
// (reduced from sv-benchmarks heap-manipulation/dll_of_dll-1.c, issue #642)

extern void abort(void);
extern int __VERIFIER_nondet_int(void);
void reach_error(void) { abort(); }

int apply(int (*f)(int), int x) { return f(x); }

int leaf(int x) { return x; }

int inner(int x) { return apply(leaf, x) + 1; }

int main(void) {
  int x = __VERIFIER_nondet_int();
  if (x < 0 || x > 100) {
    return 0;
  }
  if (apply(inner, x) != x + 1) {
    reach_error();
  }
  return 0;
}
