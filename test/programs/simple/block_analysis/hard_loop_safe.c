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
  __assert_fail("0", "hard_loop_safe.c", 24, "reach_error");
}

extern int __VERIFIER_nondet_int();

int main() {

  int x = 0; //__VERIFIER_nondet_int();
  int y = 0;

  while (x != 1000) {
    while (y != 1000) {
      y++;
    }
    x++;
  }

  if (x != 1000 || y != 1000)
    reach_error();

  return 0;
}
