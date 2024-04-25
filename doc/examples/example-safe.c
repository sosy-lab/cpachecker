// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned __VERIFIER_nondet_uint();
extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);
int main() {
  unsigned n = __VERIFIER_nondet_uint();
  unsigned x = __VERIFIER_nondet_uint();
  unsigned y = n - x;
  while (x > y) {
    x--;
    y++;
    if (x + y != n) {
      __assert_fail("0", "example-safe.c", 20, "main");
    }
  }
  return 0;
}
