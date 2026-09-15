// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <alloca.h>

extern int __VERIFIER_nondet_int(void);

int main() {
  {
  int *ptr = alloca(10*sizeof(int));

  int nondet = __VERIFIER_nondet_int();
  *ptr = nondet;
  assert(*ptr == nondet);
  }
  // Safe
}
