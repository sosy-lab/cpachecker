// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern int __VERIFIER_nondet_int(void);


int main() {
  int int_var = __VERIFIER_nondet_int();

  // Prevents underflow and restricts int_var from -4,999,999 to int max
  if (int_var <= -5000000) { // The witness (v1) is supposed to allow this to be true and encode this as a sink
    return 0;
  }

  // Restricts int_var from -5,000,000 to (signed int max - 1)
  int_var--;

  // Restricts int_var from -5,000,000 to -4,999,999
  if (int_var > -4999999) { // The witness (v1) is supposed to allow this to be true and encode this as a sink
    return 0;
  }

  assert(int_var != -5000001); // Safe

  assert(int_var != -4999998); // Safe

  // At this point int_var can only be either -5000000 or -4999999
  if (int_var == -5000000) {
    // int_var == -5000000
    return 0;
  } else {
    // int_var == -4999999
    // Do nothing, leads to ERROR
  }

  ERROR:
  return 1;
}