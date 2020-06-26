// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_assume(int);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  int i;
  __VERIFIER_assume(i == 5);

  __VERIFIER_assert(i % 2 == 1);
  __VERIFIER_assert(-i % 2 == -1);
  __VERIFIER_assert(i % -2 == 1);
  __VERIFIER_assert(-i % -2 == -1);
}
