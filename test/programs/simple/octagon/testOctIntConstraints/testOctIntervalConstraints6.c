// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: goto ERROR;
  }
  return;
}

extern unsigned int __VERIFIER_nondet_uint();

int main(void) {
  unsigned int a = __VERIFIER_nondet_uint(); //interval from 0 to infinity

  __VERIFIER_assert(a < 10); 
  return a;
}

