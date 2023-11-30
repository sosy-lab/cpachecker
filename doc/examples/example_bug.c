// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function);

extern int __VERIFIER_nondet_int(void);
int main() {
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();

  while (1) {
    if (x > 1000) {
      x--;
    } else if (x < 100) {
      x++;
    } else {
      break;
    }
  }
  while (1) {
    if (y < 0) {
      y++;
    } else if (y > 200) {
      y--;
    } else {
      break;
    }
  }

  if (x * y == 39203) {
    __assert_fail("x * y != 39203", "example_bug.c", 37, "main");
  }
}

