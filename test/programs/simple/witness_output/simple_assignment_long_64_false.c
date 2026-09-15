// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern long __VERIFIER_nondet_long(void);


// LP64 version
int main() {
  long long_var = __VERIFIER_nondet_long();

  if (long_var <= -9000000000000000000LL) { // Prevents underflow and restricts long_var from -9000000000000000000LL to long max
    return 0;
  }
  long_var--;

  if (long_var >= 9000000000000000000LL) {
    return 0;
  }

  assert(long_var != -9000000000000000001LL); // Safe
 	
  assert(long_var != -9000000000000000000LL); // Fails -> long_var == -9000000000000000000

  return 0;
}