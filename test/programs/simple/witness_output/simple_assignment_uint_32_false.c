// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern unsigned int __VERIFIER_nondet_uint(void);


int main() {
  unsigned int uint_var = __VERIFIER_nondet_uint();

  if (uint_var >= 4000000000u) { // Prevents overflow and restricts uint_var from 0 to (4000000000 - 1)
    return 0;
  }
  uint_var++;

  if (uint_var <= 3000000000u) {
    return 0;
  }

  assert(uint_var != 4000000001u); // Safe

  assert(uint_var != 4000000000u); // Fails -> uint_var == 4000000000
  
  return 0;
}