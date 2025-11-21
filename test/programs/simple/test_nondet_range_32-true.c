// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdbool.h>

extern int __VERIFIER_nondet_int();
extern short __VERIFIER_nondet_short();
extern char __VERIFIER_nondet_char();
extern bool __VERIFIER_nondet_bool();

// TODO: all other defined verifier nondet functions that make sense


// Test that we return the correct type from the nondet functions, not just a unknown value with arbitrary size/type
// This program is safe!
int main() {
  bool booleanNondet = __VERIFIER_nondet_bool(); // can only be 1 or 0
  char biggerThanMaxBool = 2;
  char smallerThanMinBool = -1;
  if (booleanNondet == biggerThanMaxBool) {
    goto ERROR;
  }
  if (booleanNondet > biggerThanMaxBool) {
    goto ERROR;
  }
  if (booleanNondet == smallerThanMinBool) {
    goto ERROR;
  }
  if (booleanNondet < smallerThanMinBool) {
    goto ERROR;
  }

  if (__VERIFIER_nondet_bool() == 2147483649LL) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_bool() == 2147483647) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_bool() == 256) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_bool() == 255) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_bool() == 100) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_bool() == biggerThanMaxBool) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_bool() > biggerThanMaxBool) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_bool() == smallerThanMinBool) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_bool() < smallerThanMinBool) {
    goto ERROR;
  }

  char charNondet = __VERIFIER_nondet_char(); // between -127 and 128 (or 0 - 255)
  short biggerThanMaxUnsignedChar = 256;
  short smallerThanMinSignedChar = -129;
  if (charNondet == biggerThanMaxUnsignedChar) {
    goto ERROR;
  }
  if (charNondet > biggerThanMaxUnsignedChar) {
    goto ERROR;
  }
  if (charNondet == smallerThanMinSignedChar) {
    goto ERROR;
  }
  if (charNondet < smallerThanMinSignedChar) {
    goto ERROR;
  }

  if (__VERIFIER_nondet_char() == biggerThanMaxUnsignedChar) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_char() > biggerThanMaxUnsignedChar) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_char() == smallerThanMinSignedChar) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_char() < smallerThanMinSignedChar) {
    goto ERROR;
  }

  short shortNondet = __VERIFIER_nondet_short(); // -32768 - 32767
  int biggerThanMaxUnsignedShort = 32768;
  int smallerThanMinSignedShort = -32770;
  if (shortNondet == biggerThanMaxUnsignedShort) {
    goto ERROR;
  }
  if (shortNondet > biggerThanMaxUnsignedShort) {
    goto ERROR;
  }
  if (shortNondet == smallerThanMinSignedShort) {
    goto ERROR;
  }
  if (shortNondet < smallerThanMinSignedShort) {
    goto ERROR;
  }

  if (__VERIFIER_nondet_short() == biggerThanMaxUnsignedShort) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_short() > biggerThanMaxUnsignedShort) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_short() == smallerThanMinSignedShort) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_short() < smallerThanMinSignedShort) {
    goto ERROR;
  }

  int intNondet = __VERIFIER_nondet_int(); // -2147483648 - 2147483647
  long long biggerThanMaxUnsignedInt = 4294967297ll;
  long long smallerThanMinSignedInt = -2147483649ll;
  if (intNondet == biggerThanMaxUnsignedInt) {
    goto ERROR;
  }
  if (intNondet > biggerThanMaxUnsignedInt) {
    goto ERROR;
  }
  if (intNondet == smallerThanMinSignedInt) {
    goto ERROR;
  }
  if (intNondet < smallerThanMinSignedInt) {
    goto ERROR;
  }

  if (__VERIFIER_nondet_int() == biggerThanMaxUnsignedInt) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_int() > biggerThanMaxUnsignedInt) {
    goto ERROR;
  }

  if (__VERIFIER_nondet_int() == 2147483648ll) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_int() > 2147483648ll) {
    goto ERROR;
  }

  if (__VERIFIER_nondet_int() == smallerThanMinSignedInt) {
    goto ERROR;
  }
  if (__VERIFIER_nondet_int() < smallerThanMinSignedInt) {
    goto ERROR;
  }

  return 0;

  ERROR:
  return 1;
}
