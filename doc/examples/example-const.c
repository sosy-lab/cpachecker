// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);
int main() {
  int x = 0;
  int y = 0;
  int z = 0;
  while (x < 2) {
    x++;
    y = z + 1;
  }
  if (z != 0) {
    __assert_fail("0", "example-const.c", 20, "main");
  }
  return 0;
}
