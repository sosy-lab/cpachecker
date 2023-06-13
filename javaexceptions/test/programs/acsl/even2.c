// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error() __attribute__ ((__noreturn__));
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
extern int __VERIFIER_nondet_int();

int main(void) {
  unsigned int x = 1;
  /*@ loop invariant  1 <= x <= 10 && x % 2 == 1;*/
  do {
    x += 1;
    __VERIFIER_assert(!(x%2));
  } while (++x < 10);
  return 0;
}
