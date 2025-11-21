// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <limits.h>

int zero = 0;

// Int
int signedIntMax = 2147483647; // == INT_MAX
int signedIntMaxMinusOne = 2147483646;
int signedIntMin = -2147483647; // INT_MIN + 1, but INT_MIN exceeds the standards defined range by -1!
int signedIntMinPlusOne = -2147483646;
int signedIntMaxHalfRoundedUp = 1073741824;
int signedIntMaxHalfRoundedDown = 1073741823;

unsigned int unsignedIntMax = 4294967295u;
unsigned int unsignedIntMaxMinusOne = 4294967294u;
unsigned int unsignedIntMaxHalfRoundedDown = 2147483647u;
unsigned int unsignedIntMaxHalfRoundedUp = 2147483648u;
unsigned int unsignedIntThird = 1431655765u;

// Long (64 bit)
long signedLongMax = 9223372036854775807L;
long signedLongMaxMinusOne = 9223372036854775806l;
long signedLongMin = -9223372036854775807L; // Same as with integer minimum, this is according to the standard. LONG_MIN is 1 less than this!
long signedLongMinPlusOne = -9223372036854775806l;
long signedLongMaxHalfRoundedUp = 4611686018427387904l;
long signedLongMaxHalfRoundedDown = 4611686018427387903l;
long signedLongMaxSeventh = 1317624576693539401l;

unsigned long unsignedLongMax = 18446744073709551615ul;
unsigned long unsignedLongMaxMinusOne = 18446744073709551614ul;
unsigned long unsignedLongMaxHalfRoundedDown = 9223372036854775807ul;
unsigned long unsignedLongMaxHalfRoundedUp = 9223372036854775808ul;
unsigned long unsignedLongThird = 6148914691236517205ul;

// Long Long
long long signedLongLongMax = 9223372036854775807ll;
long long signedLongLongMaxMinusOne = 9223372036854775806ll;
long long signedLongLongMin = -9223372036854775807LL; // Same as with integer minimum, this is according to the standard
long long signedLongLongMinPlusOne = -9223372036854775806ll;
long long signedLongLongMaxHalfRoundedUp = 4611686018427387904ll;
long long signedLongLongMaxHalfRoundedDown = 4611686018427387903ll;
long long signedLongLongMaxSeventh = 1317624576693539401l;

unsigned long long unsignedLongLongMax = 18446744073709551615ull;
unsigned long long unsignedLongLongMaxMinusOne = 18446744073709551614ull;
unsigned long long unsignedLongLongMaxHalfRoundedDown = 9223372036854775807ull;
unsigned long long unsignedLongLongMaxHalfRoundedUp = 9223372036854775808ull;
unsigned long long unsignedLongLongThird = 6148914691236517205ull;

int intArray[] = {2147483647, -2147483646};
int (* intArrayPtr)[2] = &intArray;

long long longLongArray[] = {9223372036854775807ll, -9223372036854775806ll};
long long (* longLongArrayPtr)[2] = &longLongArray;

struct intStruct {
  int intMax;
  int intMin;
} intStruct = {2147483647, -2147483646};

struct intStruct * intStructPtr = &intStruct;

struct longLongStruct {
  long long longLongMax;
  long long longLongMin;
} longLongStruct = {9223372036854775807ll, -9223372036854775806ll};

struct longLongStruct * longLongStructPtr = &longLongStruct;

int getIntMax() {
  return signedIntMax;
}

int * getIntMinPointer() {
  int * intMinPtr = malloc(sizeof(int));
  *intMinPtr = signedIntMin;
  return intMinPtr;
}

long long getLongLongMax() {
  return signedLongLongMax;
}

long long * getLongLongMinPointer() {
  long long * longLongMinPtr = malloc(sizeof(long long));
  *longLongMinPtr = signedLongLongMin;
  return longLongMinPtr;
}

// Tests GCC (GNU) builtin overflow functions
int main() {
  int overallResult = 0;

  // Sanity checks for the values used
  overallResult = overallResult || !(signedIntMax == INT_MAX);
  overallResult = overallResult || !(signedIntMaxMinusOne == INT_MAX - 1);
  overallResult = overallResult || !(signedIntMin == INT_MIN + 1);
  overallResult = overallResult || !(signedIntMinPlusOne == INT_MIN + 2);

  overallResult = overallResult || !(signedLongMax == LONG_MAX);
  overallResult = overallResult || !(signedLongMaxMinusOne == LONG_MAX - 1L);
  overallResult = overallResult || !(signedLongMin == LONG_MIN + 1L);
  overallResult = overallResult || !(signedLongMinPlusOne == LONG_MIN + 2L);

  overallResult = overallResult || !(signedLongLongMax == LONG_MAX);
  overallResult = overallResult || !(signedLongLongMaxMinusOne == LONG_MAX - 1LL);
  overallResult = overallResult || !(signedLongLongMin == LONG_MIN + 1LL);
  overallResult = overallResult || !(signedLongLongMinPlusOne == LONG_MIN + 2LL);

  // ############ Methods return the boolean overflow result, and returning the arithmetic result as a side effect ############

  {
    // __builtin_add_overflow(), types can be chosen freely
    int sIntResult;
    long sLongResult;
    long long sLongLongResult;
    unsigned int uIntResult;
    unsigned long uLongResult;
    unsigned long long uLongLongResult;

    // 0+0 == 0 (all signed int)
    overallResult = overallResult || __builtin_add_overflow(zero, zero, &sIntResult);

    // 0+0 == 0 (all unsigned int)
    overallResult = overallResult || __builtin_add_overflow(zero, zero, &uIntResult);

    // 0+0 == 0 (all signed long)
    overallResult = overallResult || __builtin_add_overflow(zero, zero, &sLongResult);

    // 0+0 == 0 (all unsigned long)
    overallResult = overallResult || __builtin_add_overflow(zero, zero, &uLongResult);

    // 0+0 == 0 (all signed long long)
    overallResult = overallResult || __builtin_add_overflow(zero, zero, &sLongLongResult);

    // 0+0 == 0 (all unsigned long long)
    overallResult = overallResult || __builtin_add_overflow(zero, zero, &uLongLongResult);

    // max+0 == max no overflow (all signed int) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedIntMax, zero, &sIntResult);
    overallResult = overallResult || __builtin_add_overflow(zero, signedIntMax, &sIntResult);

    // max+0 == no overflow (all unsigned int) + reversed
    overallResult = overallResult || __builtin_add_overflow(unsignedIntMax, zero, &uIntResult);
    overallResult = overallResult || __builtin_add_overflow(zero, unsignedIntMax, &uIntResult);

    // max+0 == no overflow (all signed long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongMax, zero, &sLongResult);
    overallResult = overallResult || __builtin_add_overflow(zero, signedLongMax, &sLongResult);

    // max+0 == no overflow (all unsigned long) + reversed
    overallResult = overallResult || __builtin_add_overflow(unsignedLongMax, zero, &uLongResult);
    overallResult = overallResult || __builtin_add_overflow(zero, unsignedLongMax, &uLongResult);

    // max+0 == no overflow (all signed long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongLongMax, zero, &sLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(zero, signedLongLongMax, &sLongLongResult);

    // max+0 == no overflow (all unsigned long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(unsignedLongLongMax, zero, &uLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(zero, unsignedLongLongMax, &uLongLongResult);

    // max+1 == overflows (all signed int) + reversed
    overallResult = overallResult || !__builtin_add_overflow(signedIntMax, 1, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(1, signedIntMax, &sIntResult);

    // max+1 == no overflow (input signed int, output unsigned int) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedIntMax, 1, &uIntResult);
    overallResult = overallResult || __builtin_add_overflow(1, signedIntMax, &uIntResult);

    // max+1 == overflows (all unsigned int) + reversed
    overallResult = overallResult || !__builtin_add_overflow(unsignedIntMax, 1u, &uIntResult);
    overallResult = overallResult || !__builtin_add_overflow(1u, unsignedIntMax, &uIntResult);

    // max+1 == overflows (all signed long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, 1l, &sLongResult);
    overallResult = overallResult || !__builtin_add_overflow(1l, signedLongMax, &sLongResult);

    // max+1 == overflows (all unsigned long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, 1ul, &uLongResult);
    overallResult = overallResult || !__builtin_add_overflow(1ul, unsignedLongMax, &uLongResult);

    // max+1 == overflows (all signed long long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, 1ll, &sLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(1ll, signedLongLongMax, &sLongLongResult);

    // max+1 == overflows (all unsigned long long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, 1ull, &uLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(1ull, unsignedLongLongMax, &uLongLongResult);

    // min+1 == min+1 (all signed int) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedIntMin, 1, &sIntResult);
    overallResult = overallResult || __builtin_add_overflow(1, signedIntMin, &sIntResult);

    // min+1 == min+1 (all unsigned int) + reversed
    overallResult = overallResult || __builtin_add_overflow(zero, 1u, &uIntResult);
    overallResult = overallResult || __builtin_add_overflow(1u, zero, &uIntResult);

    // min+1 == min+1 (all signed long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongMin, 1l, &sLongResult);
    overallResult = overallResult || __builtin_add_overflow(1l, signedLongMin, &sLongResult);

    // min+1 == min+1 (all unsigned long) + reversed
    overallResult = overallResult || __builtin_add_overflow((unsigned long)zero, 1ul, &uLongResult);
    overallResult = overallResult || __builtin_add_overflow(1ul, (unsigned long)zero, &uLongResult);

    // min+1 == min+1 (all signed long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongLongMin, 1ll, &sLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(1ll, signedLongLongMin, &sLongLongResult);

    // min+1 == min+1 (all unsigned long long) + reversed
    overallResult = overallResult || __builtin_add_overflow((unsigned long long)zero, 1ull, &uLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(1ull, (unsigned long long)zero, &uLongLongResult);

    // min+max == 0 (all signed int) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedIntMax, signedIntMin, &sIntResult);
    overallResult = overallResult || __builtin_add_overflow(signedIntMin, signedIntMax, &sIntResult);

    // min+max == max (all unsigned int) + reversed
    overallResult = overallResult || __builtin_add_overflow(zero, unsignedIntMax, &uIntResult);
    overallResult = overallResult || __builtin_add_overflow(unsignedIntMax, zero, &uIntResult);

    // min+max == 0 (all signed long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongMin, signedLongMax, &sLongResult);
    overallResult = overallResult || __builtin_add_overflow(signedLongMax, signedLongMin, &sLongResult);

    // min+max == max (all unsigned long) + reversed
    overallResult = overallResult || __builtin_add_overflow((unsigned long)zero, unsignedLongMax, &uLongResult);
    overallResult = overallResult || __builtin_add_overflow(unsignedLongMax, (unsigned long)zero, &uLongResult);

    // min+max == 0 (all signed long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongLongMin, signedLongLongMax, &sLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(signedLongLongMax, signedLongLongMin, &sLongLongResult);

    // min+max == max (all unsigned long long) + reversed
    overallResult = overallResult || __builtin_add_overflow((unsigned long long)zero, unsignedLongLongMax, &uLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(unsignedLongLongMax, (unsigned long long)zero, &uLongLongResult);

    // (min+1)+max == 2 (all signed int) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedIntMax, signedIntMinPlusOne, &sIntResult);
    overallResult = overallResult || __builtin_add_overflow(signedIntMinPlusOne, signedIntMax, &sIntResult);

    // (min+1)+max == 2 (all signed long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongMax, signedLongMinPlusOne, &sLongResult);
    overallResult = overallResult || __builtin_add_overflow(signedLongMinPlusOne, signedLongMax, &sLongResult);

    // (min+1)+max == 2 (all signed long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongLongMax, signedLongLongMinPlusOne, &sLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(signedLongLongMinPlusOne, signedLongLongMax, &sLongLongResult);

    // (max-1)+1 == max (all signed int) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedIntMaxMinusOne, 1, &sIntResult);
    overallResult = overallResult || __builtin_add_overflow(1, signedIntMaxMinusOne, &sIntResult);

    // (max-1)+1 == max (all unsigned int) + reversed
    overallResult = overallResult || __builtin_add_overflow(unsignedIntMaxMinusOne, 1u, &uIntResult);
    overallResult = overallResult || __builtin_add_overflow(1u, unsignedIntMaxMinusOne, &uIntResult);

    // (max-1)+1 == max (all signed long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongMaxMinusOne, 1l, &sLongResult);
    overallResult = overallResult || __builtin_add_overflow(1l, signedLongMaxMinusOne, &sLongResult);

    // (max-1)+1 == max (all unsigned long) + reversed
    overallResult = overallResult || __builtin_add_overflow(unsignedLongMaxMinusOne, 1ul, &uLongResult);
    overallResult = overallResult || __builtin_add_overflow(1ul, unsignedLongMaxMinusOne, &uLongResult);

    // (max-1)+1 == max (all signed long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongLongMaxMinusOne, 1ll, &sLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(1ll, signedLongLongMaxMinusOne, &sLongLongResult);

    // (max-1)+1 == max (all unsigned long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(unsignedLongLongMaxMinusOne, 1ull, &uLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(1ull, unsignedLongLongMaxMinusOne, &uLongLongResult);

    // (max-1)+2 == max+1 overflow (all signed int) + reversed
    overallResult = overallResult || !__builtin_add_overflow(signedIntMaxMinusOne, 2, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(2, signedIntMaxMinusOne, &sIntResult);

    // (smax-1)+2 == smax+1 (input signed int, output unsigned int) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedIntMaxMinusOne, 2, &uIntResult);
    overallResult = overallResult || __builtin_add_overflow(2, signedIntMaxMinusOne, &uIntResult);

    // (max-1)+1 == max (all unsigned int) + reversed
    overallResult = overallResult || __builtin_add_overflow(unsignedIntMaxMinusOne, 1u, &uIntResult);
    overallResult = overallResult || __builtin_add_overflow(1u, unsignedIntMaxMinusOne, &uIntResult);

    // (max-1)+1 == max (all signed long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongMaxMinusOne, 1l, &sLongResult);
    overallResult = overallResult || __builtin_add_overflow(1l, signedLongMaxMinusOne, &sLongResult);

    // (max-1)+1 == max (all unsigned long) + reversed
    overallResult = overallResult || __builtin_add_overflow(unsignedLongMaxMinusOne, 1ul, &uLongResult);
    overallResult = overallResult || __builtin_add_overflow(1ul, unsignedLongMaxMinusOne, &uLongResult);

    // (max-1)+1 == max (all signed long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongLongMaxMinusOne, 1ll, &sLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(1ll, signedLongLongMaxMinusOne, &sLongLongResult);

    // (max-1)+1 == max (all unsigned long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(unsignedLongLongMaxMinusOne, 1ull, &uLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(1ull, unsignedLongLongMaxMinusOne, &uLongLongResult);

    // (max-1)+2 == max+1 overflow (all unsigned int) + reversed
    overallResult = overallResult || !__builtin_add_overflow(unsignedIntMaxMinusOne, 2u, &uIntResult);
    overallResult = overallResult || !__builtin_add_overflow(2u, unsignedIntMaxMinusOne, &uIntResult);

    // (max-1)+2 == max+1 overflow (all signed long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(signedLongMaxMinusOne, 2l, &sLongResult);
    overallResult = overallResult || !__builtin_add_overflow(2l, signedLongMaxMinusOne, &sLongResult);

    // (smax-1)+2 == smax+1 (input signed long, output unsigned long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongMaxMinusOne, 2l, &uLongResult);
    overallResult = overallResult || __builtin_add_overflow(2l, signedLongMaxMinusOne, &uLongResult);

    // (max-1)+2 == max+1 overflow (all unsigned long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMaxMinusOne, 2ul, &uLongResult);
    overallResult = overallResult || !__builtin_add_overflow(2ul, unsignedLongMaxMinusOne, &uLongResult);

    // (max-1)+2 == max+1 overflow (all signed long long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMaxMinusOne, 2ll, &sLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(2ll, signedLongLongMaxMinusOne, &sLongLongResult);

    // (smax-1)+2 == smax+1 (input signed long long, output unsigned long long) + reversed
    overallResult = overallResult || __builtin_add_overflow(signedLongLongMaxMinusOne, 2ll, &uLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(2ll, signedLongLongMaxMinusOne, &uLongLongResult);

    // (max-1)+2 == max+1 overflow (all unsigned long long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMaxMinusOne, 2ull, &uLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(2ull, unsignedLongLongMaxMinusOne, &uLongLongResult);

    // (max-1)+max == max overflow (all signed int) + reversed
    overallResult = overallResult || !__builtin_add_overflow(signedIntMaxMinusOne, signedIntMax, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(signedIntMax, signedIntMaxMinusOne, &sIntResult);

    // (max-1)+max == max overflow (all unsigned int) + reversed
    overallResult = overallResult || !__builtin_add_overflow(unsignedIntMaxMinusOne, unsignedIntMax, &uIntResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedIntMax, unsignedIntMaxMinusOne, &uIntResult);

    // (max-1)+max == max overflow (all signed long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(signedLongMaxMinusOne, signedLongMax, &sLongResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, signedLongMaxMinusOne, &sLongResult);

    // (max-1)+max == max overflow (all unsigned long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMaxMinusOne, unsignedLongMax, &uLongResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMaxMinusOne, &uLongResult);

    // (max-1)+max == max overflow (all signed long long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMaxMinusOne, signedLongLongMax, &sLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, signedLongLongMaxMinusOne, &sLongLongResult);

    // (max-1)+max == max overflow (all unsigned long long) + reversed
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMaxMinusOne, unsignedLongLongMax, &uLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMaxMinusOne, &uLongLongResult);

    // max+max == max overflow (all signed int)
    overallResult = overallResult || !__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult);

    // max+max == max overflow (all unsigned int)
    overallResult = overallResult || !__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &uIntResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &uIntResult);

    // max+max == max overflow (all signed long)
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult);

    // max+max == max overflow (all unsigned long)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &uLongResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &uLongResult);

    // max+max == max overflow (all signed long long)
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult);

    // max+max == max overflow (all unsigned long long)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &uLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &uLongLongResult);

    // max+0 == max (input signed int, output signed int)
    overallResult = overallResult || __builtin_add_overflow(signedIntMax, zero, &sIntResult);
    overallResult = overallResult || __builtin_add_overflow(zero, signedIntMax, &sIntResult);

    // max+0 == max overflow (input unsigned int, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(unsignedIntMax, 0, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(0, unsignedIntMax, &sIntResult);

    // max+0 == max overflow (input signed long, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, 0, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(0, signedLongMax, &sIntResult);

    // max+0 == max overflow (input unsigned long, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, 0ul, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(0ul, unsignedLongMax, &sIntResult);

    // max+0 == max overflow (input signed long long, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, 0ll, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(0ll, signedLongLongMax, &sIntResult);

    // max+0 == max overflow (input unsigned long long, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, 0ull, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(0ull, unsignedLongLongMax, &sIntResult);

    // max+max == max overflow (input signed int, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult);

    // max+max == max overflow (input unsigned int, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sIntResult);

    // max+max == max overflow (input signed long, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, signedLongMax, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, signedLongMax, &sIntResult);

    // max+max == max overflow (input unsigned long, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sIntResult);

    // max+max == max overflow (input signed long long, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sIntResult);

    // max+max == max overflow (input unsigned long long, output signed int)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sIntResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sIntResult);

    // max+max == 2*max signed int (input signed int, output signed long)
    overallResult = overallResult || !__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult);

    // max+max == 2*max unsigned int (input unsigned int, output signed long)
    overallResult = overallResult || __builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongResult);
    overallResult = overallResult || __builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongResult);

    // max+max == max overflow (input signed long, output signed long)
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult);

    // max+max == max overflow (input unsigned long, output signed long)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongResult);

    // max+max == max overflow (input signed long long, output signed long)
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongResult);

    // max+max == max overflow (input unsigned long long, output signed long)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongResult);

    // max+max == 2*max signed int (input signed int, output signed long long)
    overallResult = overallResult || __builtin_add_overflow(signedIntMax, signedIntMax, &sLongLongResult);

    // max+max == 2*max unsigned int (input unsigned int, output signed long long)
    overallResult = overallResult || __builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongLongResult);
    overallResult = overallResult || __builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongLongResult);

    // max+max == max overflow (input signed long, output signed long long)
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, signedLongMax, &sLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongMax, signedLongMax, &sLongLongResult);

    // max+max == max overflow (input unsigned long, output signed long long)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongLongResult);

    // max+max == max overflow (input signed long long, output signed long long)
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult);

    // max+max == max overflow (input unsigned long long, output signed long long)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongLongResult);
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongLongResult);

    // max+max == 2*max signed long (input signed long, output unsigned long long)
    overallResult = overallResult || __builtin_add_overflow(signedLongMax, signedLongMax, &uLongLongResult);

    // max+max == max overflow (input unsigned long, output unsigned long long)
    overallResult = overallResult || !__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &uLongLongResult);

    // __builtin_sadd_overflow, signed int
    overallResult = overallResult || __builtin_sadd_overflow(zero, zero, &sIntResult);
    overallResult = overallResult || __builtin_sadd_overflow(signedIntMax, zero, &sIntResult);
    overallResult = overallResult || __builtin_sadd_overflow(zero, signedIntMax, &sIntResult);
    overallResult = overallResult || !__builtin_sadd_overflow(signedIntMax, 1, &sIntResult);
    overallResult = overallResult || !__builtin_sadd_overflow(1, signedIntMax, &sIntResult);
    overallResult = overallResult || __builtin_sadd_overflow(signedIntMin, 1, &sIntResult);
    overallResult = overallResult || __builtin_sadd_overflow(1, signedIntMin, &sIntResult);
    overallResult = overallResult || !__builtin_sadd_overflow(INT_MIN, -1, &sIntResult);

    // __builtin_saddl_overflow, signed long
    overallResult = overallResult || __builtin_saddl_overflow(0l, 0l, &sLongResult);
    overallResult = overallResult || __builtin_saddl_overflow(signedLongMax, 0l, &sLongResult);
    overallResult = overallResult || !__builtin_saddl_overflow(signedLongMax, 1l, &sLongResult);
    overallResult = overallResult || __builtin_saddl_overflow(signedLongMin, 1l, &sLongResult);

    // __builtin_saddll_overflow, signed long long
    overallResult = overallResult || __builtin_saddll_overflow(0ll, 0ll, &sLongLongResult);
    overallResult = overallResult || __builtin_saddll_overflow(signedLongLongMax, 0ll, &sLongLongResult);
    overallResult = overallResult || !__builtin_saddll_overflow(signedLongLongMax, 1ll, &sLongLongResult);
    overallResult = overallResult || __builtin_saddll_overflow(signedLongLongMin, 1ll, &sLongLongResult);

    // __builtin_uadd_overflow, unsigned int
    overallResult = overallResult || __builtin_uadd_overflow(0u, 0u, &uIntResult);
    overallResult = overallResult || __builtin_uadd_overflow(unsignedIntMax, 0u, &uIntResult);
    overallResult = overallResult || !__builtin_uadd_overflow(unsignedIntMax, 1u, &uIntResult);
    overallResult = overallResult || !__builtin_uadd_overflow(1u, unsignedIntMax, &uIntResult);

    // __builtin_uaddl_overflow, unsigned long
    overallResult = overallResult || __builtin_uaddl_overflow(0ul, 0ul, &uLongResult);
    overallResult = overallResult || __builtin_uaddl_overflow(unsignedLongMax, 0ul, &uLongResult);
    overallResult = overallResult || !__builtin_uaddl_overflow(unsignedLongMax, 1ul, &uLongResult);
    overallResult = overallResult || !__builtin_uaddl_overflow(1ul, unsignedLongMax, &uLongResult);

    // __builtin_uaddll_overflow, unsigned long long
    overallResult = overallResult || __builtin_uaddll_overflow(0ull, 0ull, &uLongLongResult);
    overallResult = overallResult || __builtin_uaddll_overflow(unsignedLongLongMax, 0ull, &uLongLongResult);
    overallResult = overallResult || !__builtin_uaddll_overflow(unsignedLongLongMax, 1ull, &uLongLongResult);
    overallResult = overallResult || !__builtin_uaddll_overflow(1ull, unsignedLongLongMax, &uLongLongResult);

    // __builtin_sub_overflow(), types can be chosen freely
    overallResult = overallResult || __builtin_sub_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult);
    overallResult = overallResult || __builtin_sub_overflow(signedLongLongMax, 0, &sLongLongResult);
    overallResult = overallResult || __builtin_sub_overflow(0, signedLongLongMax, &sLongLongResult);
    overallResult = overallResult || __builtin_sub_overflow(0, 0, &sLongLongResult);
    overallResult = overallResult || __builtin_sub_overflow(signedLongLongMin, -1, &sLongLongResult);

    // __builtin_ssub_overflow
    overallResult = overallResult || __builtin_ssub_overflow(zero, zero, &sIntResult);
    overallResult = overallResult || __builtin_ssub_overflow(signedIntMax, zero, &sIntResult);
    overallResult = overallResult || __builtin_ssub_overflow(signedIntMin, zero, &sIntResult);
    overallResult = overallResult || __builtin_ssub_overflow(signedIntMin, 1, &sIntResult);
    overallResult = overallResult || !__builtin_ssub_overflow(signedIntMin, 2, &sIntResult);

    // __builtin_ssubl_overflow
    overallResult = overallResult || __builtin_ssubl_overflow(0l, 0l, &sLongResult);
    overallResult = overallResult || __builtin_ssubl_overflow(signedLongMax, 0l, &sLongResult);
    overallResult = overallResult || __builtin_ssubl_overflow(signedLongMin, 0l, &sLongResult);
    overallResult = overallResult || __builtin_ssubl_overflow(signedLongMin, 1l, &sLongResult);
    overallResult = overallResult || !__builtin_ssubl_overflow(signedLongMin, 2l, &sLongResult);

    // __builtin_ssubll_overflow
    overallResult = overallResult || __builtin_ssubll_overflow(0ll, 0ll, &sLongLongResult);
    overallResult = overallResult || __builtin_ssubll_overflow(signedLongLongMax, 0ll, &sLongLongResult);
    overallResult = overallResult || __builtin_ssubll_overflow(signedLongLongMin, 0ll, &sLongLongResult);
    overallResult = overallResult || __builtin_ssubll_overflow(signedLongLongMin, 1ll, &sLongLongResult);
    overallResult = overallResult || !__builtin_ssubll_overflow(signedLongLongMin, 2ll, &sLongLongResult);

    // __builtin_usub_overflow
    overallResult = overallResult || __builtin_usub_overflow(0u, 0u, &uIntResult);
    overallResult = overallResult || __builtin_usub_overflow(unsignedIntMax, 0u, &uIntResult);
    overallResult = overallResult || !__builtin_usub_overflow(0u, 1u, &uIntResult);

    // __builtin_usubl_overflow
    overallResult = overallResult || __builtin_usubl_overflow(0ul, 0ul, &uLongResult);
    overallResult = overallResult || __builtin_usubl_overflow(unsignedLongMax, 0ul, &uLongResult);
    overallResult = overallResult || !__builtin_usubl_overflow(0ul, 1ul, &uLongResult);

    // __builtin_usubll_overflow
    overallResult = overallResult || __builtin_usubll_overflow(0ull, 0ull, &uLongLongResult);
    overallResult = overallResult || __builtin_usubll_overflow(unsignedLongLongMax, 0ull, &uLongLongResult);
    overallResult = overallResult || !__builtin_usubll_overflow(0ull, 1ull, &uLongLongResult);

    // __builtin_mul_overflow(), types can be chosen freely

    // 0*0 == 0 (all signed int)
    overallResult = overallResult || __builtin_mul_overflow(zero, zero, &sIntResult);
    // 0*0 == 0 (all unsigned int)
    overallResult = overallResult || __builtin_mul_overflow((unsigned int)zero, (unsigned int)zero, &uIntResult);
    // 0*0 == 0 (all signed long)
    overallResult = overallResult || __builtin_mul_overflow((long)zero, (long)zero, &sLongResult);
    // 0*0 == 0 (all unsigned long)
    overallResult = overallResult || __builtin_mul_overflow((unsigned long)zero, (unsigned long)zero, &uLongResult);

    // max*0 == 0 (all signed int) + reversed
    overallResult = overallResult || __builtin_mul_overflow(signedIntMax, zero, &sIntResult);
    overallResult = overallResult || __builtin_mul_overflow(zero, signedIntMax, &sIntResult);

    // max*0 == 0 (all unsigned int) + reversed
    overallResult = overallResult || __builtin_mul_overflow(unsignedIntMax, (unsigned int)zero, &uIntResult);
    overallResult = overallResult || __builtin_mul_overflow((unsigned int)zero, unsignedIntMax, &uIntResult);

    // max*0 == 0 (all signed long) + reversed
    overallResult = overallResult || __builtin_mul_overflow(signedLongMax, (long)zero, &sLongResult);
    overallResult = overallResult || __builtin_mul_overflow((long)zero, signedLongMax, &sLongResult);

    // max*0 == 0 (all unsigned long) + reversed
    overallResult = overallResult || __builtin_mul_overflow(unsignedLongMax, (unsigned long)zero, &uLongResult);
    overallResult = overallResult || __builtin_mul_overflow((unsigned long)zero, unsignedLongMax, &uLongResult);

    // max*1 == max (all signed int) + reversed
    overallResult = overallResult || __builtin_mul_overflow(signedIntMax, 1, &sIntResult);
    overallResult = overallResult || __builtin_mul_overflow(1, signedIntMax, &sIntResult);

    // max*1 == max (all unsigned int) + reversed
    overallResult = overallResult || __builtin_mul_overflow(unsignedIntMax, 1u, &uIntResult);
    overallResult = overallResult || __builtin_mul_overflow(1u, unsignedIntMax, &uIntResult);

    // max*1 == max (all signed long) + reversed
    overallResult = overallResult || __builtin_mul_overflow(signedLongMax, 1l, &sLongResult);
    overallResult = overallResult || __builtin_mul_overflow(1l, signedLongMax, &sLongResult);

    // max*1 == max (all unsigned long) + reversed
    overallResult = overallResult || __builtin_mul_overflow(unsignedLongMax, 1ul, &uLongResult);
    overallResult = overallResult || __builtin_mul_overflow(1ul, unsignedLongMax, &uLongResult);

    // max*2 == overflow (all signed int)
    overallResult = overallResult || !__builtin_mul_overflow(signedIntMax, 2, &sIntResult);
    overallResult = overallResult || !__builtin_mul_overflow(2, signedIntMax, &sIntResult);

    // max*2 == overflow (all unsigned int)
    overallResult = overallResult || !__builtin_mul_overflow(unsignedIntMax, 2u, &uIntResult);
    overallResult = overallResult || !__builtin_mul_overflow(2u, unsignedIntMax, &uIntResult);

    // max*2 == overflow (all signed long)
    overallResult = overallResult || !__builtin_mul_overflow(signedLongMax, 2l, &sLongResult);
    overallResult = overallResult || !__builtin_mul_overflow(2l, signedLongMax, &sLongResult);

    // max*2 == overflow (all unsigned long)
    overallResult = overallResult || !__builtin_mul_overflow(unsignedLongMax, 2ul, &uLongResult);
    overallResult = overallResult || !__builtin_mul_overflow(2ul, unsignedLongMax, &uLongResult);

    // long or long long into long/long long
    overallResult = overallResult || !__builtin_mul_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(signedLongLongMax, 0, &sLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(0, longLongArray[0], &sLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(signedLongLongMax, 1, &sLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(1, getLongLongMax(), &sLongLongResult);

    // long or long long into unsigned long/unsigned long long
    overallResult = overallResult || !__builtin_mul_overflow(signedLongLongMax, signedLongLongMax, &uLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(signedLongLongMax, 0, &uLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(signedLongLongMax, 1, &uLongLongResult);

    // unsigned long or unsigned long long into long/long long
    overallResult = overallResult || !__builtin_mul_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(unsignedLongLongMax, 0, &sLongLongResult);
    overallResult = overallResult || !__builtin_mul_overflow(unsignedLongLongMax, 1, &sLongLongResult);

    // unsigned long/unsigned long long into unsigned long/unsigned long long
    overallResult = overallResult || !__builtin_mul_overflow(unsignedLongLongMax, unsignedLongLongMax, &uLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(unsignedLongLongMax, 0, &uLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(unsignedLongLongMax, 1, &uLongLongResult);

    // Cases in which the multiplication fits into the larger target type
    // int max + int max == int max² in signed/unsigned long/long long
    overallResult = overallResult || __builtin_mul_overflow(signedIntMax, signedIntMax, &sLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(signedIntMax, signedIntMax, &uLongLongResult);

    // s int max * u int max into signed/unsigned long long
    overallResult = overallResult || __builtin_mul_overflow(signedIntMax, unsignedIntMax, &sLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(signedIntMax, unsignedIntMax, &uLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(unsignedIntMax, signedIntMax, &sLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(unsignedIntMax, signedIntMax, &uLongLongResult);

    // s int max * s int min into signed/unsigned long long
    overallResult = overallResult || __builtin_mul_overflow(signedIntMax, signedIntMin, &sLongLongResult);
    overallResult = overallResult || !__builtin_mul_overflow(signedIntMax, signedIntMin, &uLongLongResult);
    overallResult = overallResult || __builtin_mul_overflow(signedIntMin, signedIntMax, &sLongLongResult);
    overallResult = overallResult || !__builtin_mul_overflow(signedIntMin, signedIntMax, &uLongLongResult);

    // Test that we can insert values also via pointer-deref, arrays, functions, structs etc.
    overallResult = overallResult || !__builtin_mul_overflow(*getIntMinPointer(), getIntMax(), &uLongLongResult);
    overallResult = overallResult || !__builtin_mul_overflow(intArray[1], (*intArrayPtr)[0], &uLongLongResult);
    overallResult = overallResult || !__builtin_mul_overflow(*intArray, (*intArrayPtr)[1], &uLongLongResult);
    overallResult = overallResult || !__builtin_mul_overflow(intStruct.intMin, intStructPtr->intMax, &uLongLongResult);
    overallResult = overallResult || !__builtin_mul_overflow(intStruct.intMax, (*intStructPtr).intMin, &uLongLongResult);

    // __builtin_smul_overflow
    overallResult = overallResult || __builtin_smul_overflow(0, 0, &sIntResult);
    overallResult = overallResult || __builtin_smul_overflow(signedIntMax, 1, &sIntResult);
    overallResult = overallResult || !__builtin_smul_overflow(signedIntMax, 2, &sIntResult);

    // __builtin_smull_overflow
    overallResult = overallResult || __builtin_smull_overflow(0l, 0l, &sLongResult);
    overallResult = overallResult || __builtin_smull_overflow(signedLongMax, 1l, &sLongResult);
    overallResult = overallResult || !__builtin_smull_overflow(signedLongMax, 2l, &sLongResult);

    // __builtin_smulll_overflow
    overallResult = overallResult || __builtin_smulll_overflow(0ll, 0ll, &sLongLongResult);
    overallResult = overallResult || __builtin_smulll_overflow(signedLongLongMax, 1ll, &sLongLongResult);
    overallResult = overallResult || !__builtin_smulll_overflow(signedLongLongMax, 2ll, &sLongLongResult);

    // __builtin_umul_overflow
    overallResult = overallResult || __builtin_umul_overflow(0u, 0u, &uIntResult);
    overallResult = overallResult || __builtin_umul_overflow(unsignedIntMax, 1u, &uIntResult);
    overallResult = overallResult || !__builtin_umul_overflow(unsignedIntMax, 2u, &uIntResult);

    // __builtin_umull_overflow
    overallResult = overallResult || __builtin_umull_overflow(0ul, 0ul, &uLongResult);
    overallResult = overallResult || __builtin_umull_overflow(unsignedLongMax, 1ul, &uLongResult);
    overallResult = overallResult || !__builtin_umull_overflow(unsignedLongMax, 2ul, &uLongResult);

    // __builtin_umulll_overflow
    overallResult = overallResult || __builtin_umulll_overflow(0ull, 0ull, &uLongLongResult);
    overallResult = overallResult || __builtin_umulll_overflow(unsignedLongLongMax, 1ull, &uLongLongResult);
    overallResult = overallResult || !__builtin_umulll_overflow(unsignedLongLongMax, 2ull, &uLongLongResult);

    // __builtin_add_overflow_p(), types can be chosen freely
    overallResult = overallResult || !__builtin_add_overflow_p(signedIntMax, 1, (int)0);
    overallResult = overallResult || __builtin_add_overflow_p(signedIntMaxMinusOne, 1, (int)0);
    overallResult = overallResult || !__builtin_add_overflow_p(unsignedIntMax, 1u, (unsigned int)0);
    overallResult = overallResult || __builtin_add_overflow_p(unsignedIntMaxMinusOne, 1u, (unsigned int)0);
    overallResult = overallResult || __builtin_add_overflow_p(0, 0, (int)0);

    // __builtin_sub_overflow_p(), types can be chosen freely
    overallResult = overallResult || __builtin_sub_overflow_p(signedIntMax, signedIntMax, (int)0);
    overallResult = overallResult || __builtin_sub_overflow_p(signedIntMin, 1, (int)0);
    overallResult = overallResult || !__builtin_sub_overflow_p(signedIntMin, 2, (int)0);
    overallResult = overallResult || __builtin_sub_overflow_p(unsignedIntMax, unsignedIntMax, (unsigned int)0);
    overallResult = overallResult || !__builtin_sub_overflow_p((unsigned int)0, 1u, (unsigned int)0);
    overallResult = overallResult || __builtin_sub_overflow_p(0, 0, (int)0);

    // __builtin_mul_overflow_p(), types can be chosen freely
    overallResult = overallResult || __builtin_mul_overflow_p(0, signedIntMax, (int)0);
    overallResult = overallResult || __builtin_sub_overflow_p(signedIntMin, 1, (int)0);
    overallResult = overallResult || !__builtin_sub_overflow_p(signedIntMin, 2, (int)0);
    overallResult = overallResult || __builtin_mul_overflow_p(1u, unsignedIntMax, (unsigned int)0);
    overallResult = overallResult || !__builtin_mul_overflow_p(unsignedIntMax, 2u, (unsigned int)0);
    overallResult = overallResult || __builtin_mul_overflow_p(1, 1, (int)0);
  }

  if (overallResult) {
    assert(0);
    goto ERROR;
  }
  return 0;

ERROR:
  printf("Error\n");
  return 1;
}