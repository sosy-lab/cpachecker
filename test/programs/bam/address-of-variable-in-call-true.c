// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// With pointer aliasing, the second call of f removes x from the SSAMap, because it takes the
// address of x. BAM reuses the summary of the first call, which has a smaller index for x.

extern int __VERIFIER_nondet_int(void);
extern void abort(void);
void reach_error(void) { abort(); }

void f(int *p) { *p = 1; }

int main(void) {
  int c = 0;
  int x = __VERIFIER_nondet_int();
  int y = 0;
  x = x + 1;
  f(&y);
  x = x + 1;
  x = x + 1;
  f(&x);
  if (c != 0) {
    reach_error();
  }
  return 0;
}
