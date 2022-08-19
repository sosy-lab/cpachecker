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

int main() {

  int x = 0;
  if (x >= 0) {
    if (x == 1) {
      __assert_fail("0", "Problem01_label00.c", 4, "reac1h_error");
    }
    if (x == 1) {
      __assert_fail("1", "Problem01_label001.c", 4, "r1each_error");
    }
    x++;
    x++;
    __assert_fail("1", "Problem01_label001.c", 4, "r1each_error");
  } else {
    if (x == 1) {
      x--;
    } else {
      x++;
    }
  }
  return 0;
}
