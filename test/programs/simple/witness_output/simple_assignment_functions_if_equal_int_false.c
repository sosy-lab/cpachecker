// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern int __VERIFIER_nondet_int(void);


int getOneOrZero(int num) {
  if (num > 1 || num < 1) {
    return 0;
  }

  return num;
}


int getIntRestrictAndDec(int dec) {
  int var = __VERIFIER_nondet_int(); // must be equal to -1 for the error to be reached

  // Prevents underflow and restricts int_var from -4,999,999 to int max
  if (var <= -5000000) {
    return 0;
  }

  // Restricts int_var from -5,000,000 to (signed int max - 1)
  return var - dec;
}


int restrictToMinusTwoToZero(int input) {
  if (input < -2) {
    return 0;
  }
  return input;
}


int main() {
  int one = __VERIFIER_nondet_int(); // must be equal to 1 for the error to be reached

  one = getOneOrZero(one);

  if (one == 0) {
    return 0;
  }

  // one == 1
  int int_var = getIntRestrictAndDec(one);
  // int_var must be -2 after the assignment for error to be reached!

  // Restricts int_var from -5,000,000 to -4,999,999
  if (int_var >= 0) { // The witness (v1) is supposed to allow this to be true and encode this as a sink
    return 0;
  }

  assert(int_var != -5000001); // Safe

  assert(int_var != 0); // Safe

  // -5000000 <= int_var < 0 before calling restrictToMinusTwoToZero()
  int_var = restrictToMinusTwoToZero(int_var);

  // At this point int_var can only be between -2 and 0 
  if (int_var == -2) {
    return 0;
  } else if (int_var == -1) {
    // int_var == -1
    // Do nothing, leads to ERROR
  } else {
    // int_var == 0
    return 0;
  }

  ERROR:
  return 1;
}