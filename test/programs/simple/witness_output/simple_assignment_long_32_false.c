// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern long __VERIFIER_nondet_long(void);


// ILP32 version
int main() {
  long long_var = __VERIFIER_nondet_long();

  // Prevents underflow and restricts int_var from -4,999,999 to int max
  if (long_var <= -5000000) { // The witness (v1) is supposed to allow this to be true and encode this as a sink
    return;
  }
  long_var--;

  if (long_var >= 5000000) { // The witness (v1) is supposed to allow this to be true and encode this as a sink
    return;
  }

  assert(long_var != -5000001); // Safe

  assert(long_var != -5000000); // Fails -> long_var == -5000000 is possible due to -499999 - 1 == -5000000

  return 0;
}