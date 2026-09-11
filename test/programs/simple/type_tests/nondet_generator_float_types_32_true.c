// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


// This file should be kept up to date with the SV-COMPs defined __VERIFIER_nondet_X() API!
// __VERIFIER_nondet_X() defined types according to SV-COMP 2026:
// bool, char, int, int128, float, double, loff_t, long, longlong, pchar, pthread_t, sector_t, 
// short, size_t, u32, uchar, uint, uint128, ulong, ulonglong, unsigned, ushort
// This program only tests numeric types. So pchar etc. are not tested!


extern float __VERIFIER_nondet_float(void);
extern double __VERIFIER_nondet_double(void);


// TODO: add test program that tests casting to correct range (essentially this program, but starting from a type thats larger.)

// Safe for AssertionSafety only if all return the correct result
// This tests that an analysis sticks to the correct types of values in ILP32
// If you have doubts about whether this program has signed under/overflows:
// you can check for signed overflows in GCC using the flag -ftrapv (the program stops if an overflow is detected)
int main() {
  
  // Floats
  // We check that a float can not express a double, but this is not a full check for the type.
  float nondet_float = __VERIFIER_nondet_float();
  // Note on double 1.100000000000000088817841970012523233890533447265625: 
  // this number can never be expressed accurately by a float in C, not even when rounded.
  assert(nondet_float != 1.100000000000000088817841970012523233890533447265625); // Your analysis is unsound if this fails
  assert(nondet_float != -1.100000000000000088817841970012523233890533447265625); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_float() != 1.100000000000000088817841970012523233890533447265625); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_float() != -1.100000000000000088817841970012523233890533447265625); // Your analysis is unsound if this fails


  // Doubles
  // We check that a double can not express a long double, but this is not a full check for the type.
  // Note on 1.1f (and -1.1f); this float is actually 1.10000002384185791015625, which can always be expressed by a double
  double nondet_double = __VERIFIER_nondet_double();
  assert(nondet_double == 1.1L); // Your analysis is unsound if this fails
  assert(nondet_double == -1.1L); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_double() == 1.1L); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_double() == -1.1L); // Your analysis is unsound if this fails

  // We also check that a float can always be expressed by a double
  assert(1.10000002384185791015625 == 1.1f); // Your analysis is unsound if this fails
  assert(-1.10000002384185791015625 == -1.1f); // Your analysis is unsound if this fails

  // TODO: long double
  // Problem: long double can express all floats and doubles, 
  // but we can use e.g. subtraction of 2 or more long double numbers to generate a difference that can be checked.
  // TODO: The same technique should be used to definitely check float and double types!

  return 0;
}
