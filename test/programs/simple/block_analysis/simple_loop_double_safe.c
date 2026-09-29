// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int,
                           const char *) __attribute__((__nothrow__, __leaf__))
__attribute__((__noreturn__));

void reach_error() {
  __assert_fail("0", "simple_loop_double_safe.c", 18, "reach_error");
}

extern unsigned int __VERIFIER_nondet_uint();

int main() {
  unsigned int n = __VERIFIER_nondet_uint();
  int x = 0;
  int y = 0;
  while (x < n) {
    x++;
    y++;
    if (x != y) reach_error();
  }
  return 0;
}
