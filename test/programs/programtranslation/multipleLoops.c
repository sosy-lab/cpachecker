// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
extern void __VERIFIER_error() __attribute__((__noreturn__));
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
  ERROR:
    __VERIFIER_error();
  }
}
extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int);

int main() {
  int a1;
  int a2;
  int a3;

  for (int i = 0; i < __VERIFIER_nondet_int(); i++) {
    a1 = __VERIFIER_nondet_int();
  }

  // property violation possible if a2 or a3 is never set
  for (int a = 0; a < __VERIFIER_nondet_int(); a++) {
    a2 = a1;
  }

  // property violation possible if a2 or a3 is never set
  for (int b = 0; b < __VERIFIER_nondet_int(); b++) {
    a3 = a2;
  }

  for (int x = 0; x < __VERIFIER_nondet_int(); x++) {
    __VERIFIER_assert(a1 == a3);
  }

  return 0;
}
