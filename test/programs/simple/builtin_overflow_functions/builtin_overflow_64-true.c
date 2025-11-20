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
  // Sanity checks for the values used
  assert(signedIntMax == INT_MAX);
  assert(signedIntMaxMinusOne == INT_MAX - 1);
  assert(signedIntMin == INT_MIN + 1); // Due to INT_MIN being INT_MAX + 1, it exceeds the standards defined range!
  assert(signedIntMinPlusOne == INT_MIN + 2);

  assert(signedLongMax == LONG_MAX); // Fails for 32 bit long. This is a 64 bit program!
  assert(signedLongMaxMinusOne == LONG_MAX - 1L);
  assert(signedLongMin == LONG_MIN + 1L); // As above
  assert(signedLongMinPlusOne == LONG_MIN + 2L);

  assert(signedLongLongMax == LONG_MAX);
  assert(signedLongLongMaxMinusOne == LONG_MAX - 1LL);
  assert(signedLongLongMin == LONG_MIN + 1LL); // As above
  assert(signedLongLongMinPlusOne == LONG_MIN + 2LL);

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
    if (__builtin_add_overflow(zero, zero, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != zero) {
      assert(0);
      goto ERROR;
    }
    // 0+0 == 0 (all unsigned int)
    if (__builtin_add_overflow(zero, zero, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != zero) {
      assert(0);
      goto ERROR;
    }
    // 0+0 == 0 (all signed long)
    if (__builtin_add_overflow(zero, zero, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != zero) {
      assert(0);
      goto ERROR;
    }
    // 0+0 == 0 (all unsigned long)
    if (__builtin_add_overflow(zero, zero, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != zero) {
      assert(0);
      goto ERROR;
    }
    // 0+0 == 0 (all signed long long)
    if (__builtin_add_overflow(zero, zero, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != zero) {
      assert(0);
      goto ERROR;
    }
    // 0+0 == 0 (all unsigned long long)
    if (__builtin_add_overflow(zero, zero, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != zero) {
      assert(0);
      goto ERROR;
    }

    // max+0 == max no overflow (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMax, zero, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      assert(0);
      goto ERROR;
    }
    // max+0 == no overflow (all unsigned int) + reversed
    if (__builtin_add_overflow(unsignedIntMax, zero, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, unsignedIntMax, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      assert(0);
      goto ERROR;
    }
    // max+0 == no overflow (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMax, zero, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, signedLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      assert(0);
      goto ERROR;
    }
    // max+0 == no overflow (all unsigned long) + reversed
    if (__builtin_add_overflow(unsignedLongMax, zero, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, unsignedLongMax, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      assert(0);
      goto ERROR;
    }
    // max+0 == no overflow (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMax, zero, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, signedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    // max+0 == no overflow (all unsigned long long) + reversed
    if (__builtin_add_overflow(unsignedLongLongMax, zero, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, unsignedLongLongMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    // max+1 == overflows (all signed int) + reversed
    if (!__builtin_add_overflow(signedIntMax, 1, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      printf("Result: %d\n", sIntResult);
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(1, signedIntMax, &sIntResult)) {
      printf("Result: %d\n", sIntResult);
      assert(0);
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      assert(0);
      goto ERROR;
    }

    // max+1 == no overflow (input signed int, output unsigned int) + reversed
    if (__builtin_add_overflow(signedIntMax, 1, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != signedIntMax + 1) {
      printf("Result: %d\n", sIntResult);
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1, signedIntMax, &uIntResult)) {
      printf("Result: %d\n", sIntResult);
      assert(0);
      goto ERROR;
    }
    if (uIntResult != signedIntMax + 1) {
      assert(0);
      goto ERROR;
    }
    // max+1 == overflows (all unsigned int) + reversed
    if (!__builtin_add_overflow(unsignedIntMax, 1u, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + 1u)) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(1u, unsignedIntMax, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + 1u)) {
      assert(0);
      goto ERROR;
    }
    // max+1 == overflows (all signed long) + reversed
    if (!__builtin_add_overflow(signedLongMax, 1l, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != LONG_MIN) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(1l, signedLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != LONG_MIN) {
      assert(0);
      goto ERROR;
    }
    // max+1 == overflows (all unsigned long) + reversed
    if (!__builtin_add_overflow(unsignedLongMax, 1ul, &uLongResult)) {
     assert(0);
     goto ERROR;
    }
    if (uLongResult != (unsignedLongMax + 1ul)) {
     assert(0);
     goto ERROR;
    }
    if (!__builtin_add_overflow(1ul, unsignedLongMax, &uLongResult)) {
     assert(0);
     goto ERROR;
    }
    if (uLongResult != (unsignedLongMax + 1ul)) {
     assert(0);
     goto ERROR;
    }
    // max+1 == overflows (all signed long long) + reversed
    if (!__builtin_add_overflow(signedLongLongMax, 1ll, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != LONG_MIN) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(1ll, signedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != LONG_MIN) {
      assert(0);
      goto ERROR;
    }
    // max+1 == overflows (all unsigned long long) + reversed
    if (!__builtin_add_overflow(unsignedLongLongMax, 1ull, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + 1ull)) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(1ull, unsignedLongLongMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + 1ull)) {
      assert(0);
      goto ERROR;
    }
    // min+1 == min+1 (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMin, 1, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMinPlusOne) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1, signedIntMin, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMinPlusOne) {
      assert(0);
      goto ERROR;
    }
    // min+1 == min+1 (all unsigned int) + reversed
    if (__builtin_add_overflow(zero, 1u, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != 1u) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1u, zero, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != 1u) {
      assert(0);
      goto ERROR;
    }
    // min+1 == min+1 (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMin, 1l, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != signedLongMinPlusOne) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1l, signedLongMin, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != signedLongMinPlusOne) {
      assert(0);
      goto ERROR;
    }
    // min+1 == min+1 (all unsigned long) + reversed
    if (__builtin_add_overflow((unsigned long)zero, 1ul, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != 1ul) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1ul, (unsigned long)zero, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != 1ul) {
      assert(0);
      goto ERROR;
    }
    // min+1 == min+1 (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMin, 1ll, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMinPlusOne) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1ll, signedLongLongMin, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMinPlusOne) {
      assert(0);
      goto ERROR;
    }
    // min+1 == min+1 (all unsigned long long) + reversed
    if (__builtin_add_overflow((unsigned long long)zero, 1ull, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 1ull) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1ull, (unsigned long long)zero, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 1ull) {
      assert(0);
      goto ERROR;
    }

    // min+max == 1 (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMax, signedIntMin, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != 0) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(signedIntMin, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != 0) {
      assert(0);
      goto ERROR;
    }
    // min+max == max (all unsigned int) + reversed
    if (__builtin_add_overflow(zero, unsignedIntMax, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(unsignedIntMax, zero, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      assert(0);
      goto ERROR;
    }
    // min+max == 1 (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMin, signedLongMax, &sLongResult)) {
     assert(0);
     goto ERROR;
    }
    if (sLongResult != 0) {
     assert(0);
     goto ERROR;
    }
    if (__builtin_add_overflow(signedLongMax, signedLongMin, &sLongResult)) {
     assert(0);
     goto ERROR;
    }
    if (sLongResult != 0) {
     assert(0);
     goto ERROR;
    }
    // min+max == max (all unsigned long) + reversed
    if (__builtin_add_overflow((unsigned long)zero, unsignedLongMax, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(unsignedLongMax, (unsigned long)zero, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      assert(0);
      goto ERROR;
    }
    // min+max == 1 (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMin, signedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(signedLongLongMax, signedLongLongMin, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      assert(0);
      goto ERROR;
    }
    // min+max == max (all unsigned long long) + reversed
    if (__builtin_add_overflow((unsigned long long)zero, unsignedLongLongMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(unsignedLongLongMax, (unsigned long long)zero, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    // (min+1)+max == 2 (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMax, signedIntMinPlusOne, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != 1) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(signedIntMinPlusOne, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != 1) {
      assert(0);
      goto ERROR;
    }
    // (min+1)+max == 2 (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMax, signedLongMinPlusOne, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != 1l) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(signedLongMinPlusOne, signedLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != 1l) {
      assert(0);
      goto ERROR;
    }
    // (min+1)+max == 2 (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMax, signedLongLongMinPlusOne, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 1ll) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(signedLongLongMinPlusOne, signedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 1ll) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMaxMinusOne, 1, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1, signedIntMaxMinusOne, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned int) + reversed
    if (__builtin_add_overflow(unsignedIntMaxMinusOne, 1u, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1u, unsignedIntMaxMinusOne, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMaxMinusOne, 1l, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1l, signedLongMaxMinusOne, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned long) + reversed
    if (__builtin_add_overflow(unsignedLongMaxMinusOne, 1ul, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1ul, unsignedLongMaxMinusOne, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMaxMinusOne, 1ll, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1ll, signedLongLongMaxMinusOne, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned long long) + reversed
    if (__builtin_add_overflow(unsignedLongLongMaxMinusOne, 1ull, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1ull, unsignedLongLongMaxMinusOne, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all signed int) + reversed
    if (!__builtin_add_overflow(signedIntMaxMinusOne, 2, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      assert(0);
      goto ERROR;
    }

    if (!__builtin_add_overflow(2, signedIntMaxMinusOne, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      assert(0);
      goto ERROR;
    }

    // (smax-1)+2 == smax+1 (input signed int, output unsigned int) + reversed
    if (__builtin_add_overflow(signedIntMaxMinusOne, 2, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != signedIntMax+1) {
      assert(0);
      goto ERROR;
    }

    if (__builtin_add_overflow(2, signedIntMaxMinusOne, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != signedIntMax+1) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned int) + reversed
    if (__builtin_add_overflow(unsignedIntMaxMinusOne, 1u, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1u, unsignedIntMaxMinusOne, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMaxMinusOne, 1l, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1l, signedLongMaxMinusOne, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned long) + reversed
    if (__builtin_add_overflow(unsignedLongMaxMinusOne, 1ul, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1ul, unsignedLongMaxMinusOne, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMaxMinusOne, 1ll, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1ll, signedLongLongMaxMinusOne, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned long long) + reversed
    if (__builtin_add_overflow(unsignedLongLongMaxMinusOne, 1ull, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(1ull, unsignedLongLongMaxMinusOne, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all unsigned int) + reversed
    if (!__builtin_add_overflow(unsignedIntMaxMinusOne, 2u, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + 1u)) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(2u, unsignedIntMaxMinusOne, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + 1u)) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all signed long) + reversed
    if (!__builtin_add_overflow(signedLongMaxMinusOne, 2l, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != LONG_MIN) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(2l, signedLongMaxMinusOne, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != LONG_MIN) {
      assert(0);
      goto ERROR;
    }
    // (smax-1)+2 == smax+1 (input signed long, output unsigned long) + reversed
    if (__builtin_add_overflow(signedLongMaxMinusOne, 2l, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != signedLongMax + 1l) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(2l, signedLongMaxMinusOne, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != signedLongMax + 1l) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all unsigned long) + reversed
    if (!__builtin_add_overflow(unsignedLongMaxMinusOne, 2ul, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMax + 1ul)) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(2ul, unsignedLongMaxMinusOne, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMax + 1ul)) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all signed long long) + reversed
    if (!__builtin_add_overflow(signedLongLongMaxMinusOne, 2ll, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != LONG_MIN) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(2ll, signedLongLongMaxMinusOne, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != LONG_MIN) {
      assert(0);
      goto ERROR;
    }
    // (smax-1)+2 == smax+1 (input signed long long, output unsigned long long) + reversed
    if (__builtin_add_overflow(signedLongLongMaxMinusOne, 2ll, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != signedLongLongMax + 1ll) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(2ll, signedLongLongMaxMinusOne, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != signedLongLongMax + 1ll) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all unsigned long long) + reversed
    if (!__builtin_add_overflow(unsignedLongLongMaxMinusOne, 2ull, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + 1ull)) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(2ull, unsignedLongLongMaxMinusOne, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + 1ull)) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+max == max overflow (all signed int) + reversed
    if (!__builtin_add_overflow(signedIntMaxMinusOne, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -3) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedIntMax, signedIntMaxMinusOne, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -3) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+max == max overflow (all unsigned int) + reversed
    if (!__builtin_add_overflow(unsignedIntMaxMinusOne, unsignedIntMax, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMaxMinusOne + unsignedIntMax)) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedIntMax, unsignedIntMaxMinusOne, &uIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMaxMinusOne + unsignedIntMax)) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+max == max overflow (all signed long) + reversed
    if (!__builtin_add_overflow(signedLongMaxMinusOne, signedLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -3l) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongMax, signedLongMaxMinusOne, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -3l) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+max == max overflow (all unsigned long) + reversed
    if (!__builtin_add_overflow(unsignedLongMaxMinusOne, unsignedLongMax, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMaxMinusOne + unsignedLongMax)) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMaxMinusOne, &uLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMaxMinusOne + unsignedLongMax)) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+max == max overflow (all signed long long) + reversed
    if (!__builtin_add_overflow(signedLongLongMaxMinusOne, signedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != -3ll) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMaxMinusOne, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != -3ll) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+max == max overflow (all unsigned long long) + reversed
    if (!__builtin_add_overflow(unsignedLongLongMaxMinusOne, unsignedLongLongMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMaxMinusOne + unsignedLongLongMax)) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMaxMinusOne, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMaxMinusOne + unsignedLongLongMax)) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (all signed int)
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (all unsigned int)
    // max+max == max overflow (all signed long)
    // max+max == max overflow (all unsigned long)
    // max+max == max overflow (all signed long long)
    // max+max == max overflow (all unsigned long long)

    // max+0 == max (input signed int, output signed int)
    if (__builtin_add_overflow(signedIntMax, zero, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      assert(0);
      goto ERROR;
    }
    // max+0 == max overflow (input unsigned int, output signed int)
    if (!__builtin_add_overflow(unsignedIntMax, 0, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(0, unsignedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    // max+0 == max overflow (input signed long, output signed int)
    if (!__builtin_add_overflow(signedLongMax, 0, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(0, signedLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    // max+0 == max overflow (input unsigned long, output signed int)
    if (!__builtin_add_overflow(unsignedLongMax, 0ul, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(0ul, unsignedLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    // max+0 == max overflow (input signed long long, output signed int)
    if (!__builtin_add_overflow(signedLongLongMax, 0ll, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(0ll, signedLongLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    // max+0 == max overflow (input unsigned long long, output signed int)
    if (!__builtin_add_overflow(unsignedLongLongMax, 0ull, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(0ull, unsignedLongLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -1) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input signed int, output signed int)
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input unsigned int, output signed int)
    if (!__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input signed long, output signed int)
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long, output signed int)
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input signed long long, output signed int)
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long long, output signed int)
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    // max+max == 2*max signed int (input signed int, output signed long)
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != -2) {
      assert(0);
      goto ERROR;
    }
    // max+max == 2*max unsigned int (input unsigned int, output signed long)
    if (__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != 8589934590l) { // 2 * 4294967295
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != 8589934590l) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input signed long, output signed long)
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -2l) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -2l) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long, output signed long)
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -2l) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -2l) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input signed long long, output signed long)
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -2l) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -2l) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long long, output signed long)
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -2l) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult != -2l) {
      assert(0);
      goto ERROR;
    }
    // max+max == 2*max signed int (input signed int, output signed long long)
    if (__builtin_add_overflow(signedIntMax, signedIntMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 4294967294L) {
      assert(0);
      goto ERROR;
    }
    // max+max == 2*max unsigned int (input unsigned int, output signed long long)
    // max+max == max overflow (input signed long, output signed long long)
    // max+max == max overflow (input unsigned long, output signed long long)
    // max+max == max overflow (input signed long long, output signed long long)
    // max+max == max overflow (input unsigned long long, output signed long long)

    // max+max == 2*max signed long (input signed long, output unsigned long long)
    if (__builtin_add_overflow(signedLongMax, signedLongMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 18446744073709551614ULL) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long, output unsigned long long)
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax - 1ULL) {
      assert(0);
      goto ERROR;
    }

  }

  // __builtin_sadd_overflow, signed int
  {



  }

  // __builtin_saddl_overflow, signed long
  {

  }

  // __builtin_saddll_overflow, singed long long
  {

  }

  // __builtin_uadd_overflow, unsigned int
  {

  }

  // __builtin_uaddl_overflow, unsigned long
  {

  }


  // __builtin_uaddll_overflow, unsigned long long
  {

  }

  // __builtin_sub_overflow(), types can be chosen freely
  {
    int sIntResult;
    long sLongResult;
    long long sLongLongResult;
    unsigned int uIntResult;
    unsigned long uLongResult;
    unsigned long long uLongLongResult;


    if (__builtin_sub_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_sub_overflow(signedLongLongMax, 0, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_sub_overflow(0, signedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != -signedLongLongMax) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_sub_overflow(0, 0, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_sub_overflow(signedLongLongMin, -1, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != -9223372036854775806) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    
  }

  // __builtin_ssub_overflow
  {


  }
  
  // __builtin_ssubl_overflow
  {

  }

  // __builtin_ssubll_overflow
  {

  }

  // __builtin_usub_overflow
  {
 
  }

  // __builtin_usubl_overflow
  {

  }

  // __builtin_usubll_overflow
  {

  }

  // __builtin_mul_overflow(), types can be chosen freely
  {
    int sIntResult;
    long sLongResult;
    long long sLongLongResult;
    unsigned int uIntResult;
    unsigned long uLongResult;
    unsigned long long uLongLongResult;


    // TODO: missing cases


    // long or long long into long/long long
    if (!__builtin_mul_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 1) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedLongLongMax, 0, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(0, longLongArray[0], &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedLongLongMax, 1, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

        if (__builtin_mul_overflow(1, getLongLongMax(), &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }


    // long or long long into unsigned long/unsigned long long
    if (!__builtin_mul_overflow(signedLongLongMax, signedLongLongMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 1) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedLongLongMax, 0, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 0) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedLongLongMax, 1, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != signedLongLongMax) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }


    // unsigned long or unsigned long long into long/long long
    if (!__builtin_mul_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 1) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedLongLongMax, 0, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(unsignedLongLongMax, 1, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != -1) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }


    // unsigned long/unsigned long long into unsigned long/unsigned long long
    if (!__builtin_mul_overflow(unsignedLongLongMax, unsignedLongLongMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 1) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedLongLongMax, 0, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 0) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedLongLongMax, 1, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    // Cases in which the multiplication fits into the larger target type
    // int max + int max == int max² in signed/unsigned long/long long
    if (__builtin_mul_overflow(signedIntMax, signedIntMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 4611686014132420609LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedIntMax, signedIntMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 4611686014132420609LL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    // s int max * u int max into signed/unsigned long long
    if (__builtin_mul_overflow(signedIntMax, unsignedIntMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 9223372030412324865LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedIntMax, unsignedIntMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 9223372030412324865LL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedIntMax, signedIntMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 9223372030412324865LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedIntMax, signedIntMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 9223372030412324865LL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    // s int max * s int min into signed/unsigned long long
    if (__builtin_mul_overflow(signedIntMax, signedIntMin, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != -4611686014132420609LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(signedIntMax, signedIntMin, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 13835058059577131007ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedIntMin, signedIntMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != -4611686014132420609LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(signedIntMin, signedIntMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 13835058059577131007ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    // Test that we can insert values also via pointer-deref, arrays, functions, structs etc.
    if (!__builtin_mul_overflow(*getIntMinPointer(), getIntMax(), &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 13835058059577131007ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(intArray[1], (*intArrayPtr)[0], &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 13835058061724614654ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(*intArray, (*intArrayPtr)[1], &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 13835058061724614654ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(intStruct.intMin, intStructPtr->intMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 13835058061724614654ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(intStruct.intMax, (*intStructPtr).intMin, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != 13835058061724614654ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      assert(0);
      goto ERROR;
    }


  }

  // __builtin_smul_overflow
  {

  }

  // __builtin_smull_overflow
  {

  }

  // __builtin_smulll_overflow
  {

  }

  // __builtin_umul_overflow
  {
   

  }

  // __builtin_umull_overflow
  {
   

  }

  // __builtin_smulll_overflow
  {
    

  }


  // ############ Methods checking only for overflow, not returning the arithmetic result ############

  {
    // __builtin_add_overflow_p(), types can be chosen freely
    int sIntResult;
    long sLongResult;
    long long sLongLongResult;
    unsigned int uIntResult;
    unsigned long uLongResult;
    unsigned long long uLongLongResult;

  }

  {
    // __builtin_sub_overflow_p(), types can be chosen freely
    int sIntResult;
    long sLongResult;
    long long sLongLongResult;
    unsigned int uIntResult;
    unsigned long uLongResult;
    unsigned long long uLongLongResult;

  }

  {
    // __builtin_mul_overflow_p(), types can be chosen freely
    int sIntResult;
    long sLongResult;
    long long sLongLongResult;
    unsigned int uIntResult;
    unsigned long uLongResult;
    unsigned long long uLongLongResult;

  }


  // ############ Methods returning the arithmetic result directly ############
  // (They also have a third argument that is added/subtracted, and return the boolean overflow result using a side effect)

  {
    // __builtin_addc(), unsigned int

  }

  {
    // __builtin_addcl(), unsigned long

  }

  {
    // __builtin_addcll(), unsigned long long

  }

  {
    // __builtin_subc(), unsigned int

  }

  {
    // __builtin_subcl(), unsigned long

  }

  {
    // __builtin_subcll(), unsigned long long

  }

  return 0;

  ERROR:
  printf("Error\n");
  return 1;
}
