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
  unsigned int x = __VERIFIER_nondet_uint();
  unsigned int y = x;
  unsigned int z = __VERIFIER_nondet_uint();
  while (x < 2) {
    x++;
    y++;
    z = x + z;
  }
  if (x != y) {
    __assert_fail("0", "example-sym.c", 22, "main");
  }
  return 0;
}
