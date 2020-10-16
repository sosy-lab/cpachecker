// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error() __attribute__((__noreturn__));
void __VERIFIER_assert(int cond) {
    if (!cond) {
      ERROR: __VERIFIER_error();
    }
    return;
}
extern int __VERIFIER_nondet_int();
int main() {
  int n = __VERIFIER_nondet_int();
  __VERIFIER_assert((2*n) != 0);
}
