// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *)
    __attribute__((__nothrow__, __leaf__)) __attribute__((__noreturn__));
void reach_error() { __assert_fail("0", "parameter-at-start-of-body.c", 13, "reach_error"); }
extern int __VERIFIER_nondet_int(void);

int aux(int x) {
  return x + 1;
}

int step(int dir)
{
  int r = aux(dir);
  if (r > 2) {
    reach_error();
  }
  return r;
}

int main() {
  while (__VERIFIER_nondet_int()) {
    int n = __VERIFIER_nondet_int();
    aux(n);
    step(n > 0);
  }
  return 0;
}
