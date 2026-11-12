// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail(const char *, const char *, unsigned int,
                          const char *) __attribute__((__nothrow__, __leaf__))
__attribute__((__noreturn__));

void reach_error() { __assert_fail("0", "many-ifs.c", 12, "reach_error"); }

int main() {

  int x = 0;
  if (x >= 0) {
    if (x == 1) {
      reach_error();
    }
    if (x == 1) {
      reach_error();
    }
    x++;
    x++;
    reach_error();
  } else {
    if (x == 1) {
      x--;
    } else {
      x++;
    }
  }
  return 0;
}
