// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern unsigned long __VERIFIER_nondet_ulong(void);


// ILP32 version
int main() {
  unsigned long ulong_var = __VERIFIER_nondet_ulong();

  if (ulong_var >= 4000000000u) { // Prevents overflow and restricts ulong_var from 0 to (4000000000 - 1)
    return 0;
  }
  ulong_var++;

  if (ulong_var <= 3000000000u) {
    return 0;
  }

  assert(ulong_var != 4000000001u); // Safe

  assert(ulong_var != 4000000000u); // Fails -> ulong_var == 4000000000

  return 0;
}