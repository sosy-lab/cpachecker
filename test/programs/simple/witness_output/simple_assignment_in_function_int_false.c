// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern int __VERIFIER_nondet_int(void);


void assertInt() {
  int int_var = __VERIFIER_nondet_int();

  // Prevents underflow and restricts int_var from -4,999,999 to int max
  if (int_var <= -5000000) { // The witness (v1) is supposed to allow this to be true and encode this as a sink
    return 0;
  }
  int_var--;

  if (int_var >= 5000000) { // The witness (v1) is supposed to allow this to be true and encode this as a sink
    return 0;
  }

  assert(int_var != -5000001); // Safe

  assert(int_var != -5000000); // Fails -> int_var == -5000000 is possible due to -499999 - 1 == -5000000
}

int main() {
  assertInt();
  return 0;
}