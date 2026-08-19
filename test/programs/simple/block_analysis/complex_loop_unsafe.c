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
  __assert_fail("0", "complex_loop_unsafe.c", 15, "reach_error");
}

int main() {

  int x = 5;
  if (x != 5) {
    while (x != 0) {
    LOOP:
      x--;
    }
    reach_error();
  }
  goto LOOP;
}
