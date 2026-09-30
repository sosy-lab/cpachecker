// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A loop in a function with two callers. An induction step at the loop head
// starts with an arbitrary callstack, so its paths may return to either caller.
// Both calls return their argument.

extern void __assert_fail(const char *, const char *, unsigned int, const char *)
    __attribute__((__nothrow__, __leaf__)) __attribute__((__noreturn__));
void reach_error() { __assert_fail("0", "loop-in-function-with-two-callers-1.c", 16, "reach_error"); }
extern int __VERIFIER_nondet_int(void);

int count(int n) {
  int i = 0;
  while (i < n) {
    i++;
  }
  return i;
}

int main() {
  int x = __VERIFIER_nondet_int();
  if (x < 0 || x > 1000000) {
    return 0;
  }
  int a = count(x);
  if (a != x) {
    reach_error();
  }
  int b = count(x + 1);
  if (b != x + 1) {
    reach_error();
  }
  return 0;
}
