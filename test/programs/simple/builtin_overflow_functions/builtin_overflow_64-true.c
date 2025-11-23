// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>
#include <stdlib.h>
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
  if (intMinPtr == 0) {
    exit(1);
  }
  *intMinPtr = signedIntMin;
  return intMinPtr;
}

long long getLongLongMax() {
  return signedLongLongMax;
}

long long * getLongLongMinPointer() {
  long long * longLongMinPtr = malloc(sizeof(long long));
  if (longLongMinPtr == 0) {
    exit(1);
  }
  *longLongMinPtr = signedLongLongMin;
  return longLongMinPtr;
}

// Tests GCC (GNU) builtin overflow functions for 64 bit arch
// This program is meant for error-label spec! Not assertions!
int main() {
  // Sanity checks for the values used.
  // Help understand the relation of values produced by overflows and max/min values. 
  // Also makes sure that the correct architecture is used!
  // assert(signedIntMax == INT_MAX);
  // assert(signedIntMaxMinusOne == INT_MAX - 1);
  // assert(signedIntMin == INT_MIN + 1); // Due to INT_MIN being INT_MAX + 1, it exceeds the standards defined range!
  // assert(signedIntMinPlusOne == INT_MIN + 2);

  // assert(signedLongMax == LONG_MAX); // Fails for 32 bit long. This is a 64 bit program!
  // assert(signedLongMaxMinusOne == LONG_MAX - 1L);
  // assert(signedLongMin == LONG_MIN + 1L); // As above
  // assert(signedLongMinPlusOne == LONG_MIN + 2L);

  // assert(signedLongLongMax == LONG_MAX);
  // assert(signedLongLongMaxMinusOne == LONG_MAX - 1LL);
  // assert(signedLongLongMin == LONG_MIN + 1LL); // As above
  // assert(signedLongLongMinPlusOne == LONG_MIN + 2LL);

  // Assertions for error label
  if (signedIntMax != INT_MAX) {
    goto ERROR;
  }
  if (signedIntMaxMinusOne != INT_MAX - 1) {
    goto ERROR;
  }  
  if (signedIntMin != INT_MIN + 1) { // Due to INT_MIN being INT_MAX + 1, it exceeds the standards defined range!
    goto ERROR;
  }
  if (signedIntMinPlusOne != INT_MIN + 2) {
    goto ERROR;
  }
  if (signedLongMax != LONG_MAX) { // Fails for 32 bit long. This is a 64 bit program!
    goto ERROR;
  }
  if (signedLongMaxMinusOne != LONG_MAX - 1L) {
    goto ERROR;
  }  
  if (signedLongMin != LONG_MIN + 1L) { // As above
    goto ERROR;
  }
  if (signedLongMinPlusOne != LONG_MIN + 2L) {
    goto ERROR;
  }
  if (signedLongLongMax != LONG_MAX) {
    goto ERROR;
  }  
  if (signedLongLongMaxMinusOne != LONG_MAX - 1LL) {
    goto ERROR;
  }  
  if (signedLongLongMin != LONG_MIN + 1LL) { // As above
    goto ERROR;
  }
  if (signedLongLongMinPlusOne != LONG_MIN + 2LL) {
    goto ERROR;
  }

  
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
      goto ERROR;
    }
    if (sIntResult != zero) {
      goto ERROR;
    }
    // 0+0 == 0 (all unsigned int)
    if (__builtin_add_overflow(zero, zero, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != zero) {
      goto ERROR;
    }
    // 0+0 == 0 (all signed long)
    if (__builtin_add_overflow(zero, zero, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != zero) {
      goto ERROR;
    }
    // 0+0 == 0 (all unsigned long)
    if (__builtin_add_overflow(zero, zero, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != zero) {
      goto ERROR;
    }
    // 0+0 == 0 (all signed long long)
    if (__builtin_add_overflow(zero, zero, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != zero) {
      goto ERROR;
    }
    // 0+0 == 0 (all unsigned long long)
    if (__builtin_add_overflow(zero, zero, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != zero) {
      goto ERROR;
    }

    // max+0 == max no overflow (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMax, zero, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    // max+0 == no overflow (all unsigned int) + reversed
    if (__builtin_add_overflow(unsignedIntMax, zero, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, unsignedIntMax, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    // max+0 == no overflow (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMax, zero, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      goto ERROR;
    }
    // max+0 == no overflow (all unsigned long) + reversed
    if (__builtin_add_overflow(unsignedLongMax, zero, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, unsignedLongMax, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    // max+0 == no overflow (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMax, zero, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      goto ERROR;
    }
    // max+0 == no overflow (all unsigned long long) + reversed
    if (__builtin_add_overflow(unsignedLongLongMax, zero, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, unsignedLongLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    // max+1 == overflows (all signed int) + reversed
    if (!__builtin_add_overflow(signedIntMax, 1, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      printf("Result: %d\n", sIntResult);
      goto ERROR;
    }
    if (!__builtin_add_overflow(1, signedIntMax, &sIntResult)) {
      printf("Result: %d\n", sIntResult);
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      goto ERROR;
    }

    // max+1 == no overflow (input signed int, output unsigned int) + reversed
    if (__builtin_add_overflow(signedIntMax, 1, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != signedIntMax + 1) {
      printf("Result: %d\n", sIntResult);
      goto ERROR;
    }
    if (__builtin_add_overflow(1, signedIntMax, &uIntResult)) {
      printf("Result: %d\n", sIntResult);
      goto ERROR;
    }
    if (uIntResult != signedIntMax + 1) {
      goto ERROR;
    }
    // max+1 == overflows (all unsigned int) + reversed
    if (!__builtin_add_overflow(unsignedIntMax, 1u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + 1u)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(1u, unsignedIntMax, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + 1u)) {
      goto ERROR;
    }
    // max+1 == overflows (all signed long) + reversed
    if (!__builtin_add_overflow(signedLongMax, 1l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != LONG_MIN) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(1l, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != LONG_MIN) {
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
      goto ERROR;
    }
    if (sLongLongResult != LONG_MIN) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(1ll, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != LONG_MIN) {
      goto ERROR;
    }
    // max+1 == overflows (all unsigned long long) + reversed
    if (!__builtin_add_overflow(unsignedLongLongMax, 1ull, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + 1ull)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(1ull, unsignedLongLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + 1ull)) {
      goto ERROR;
    }
    // min+1 == min+1 (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMin, 1, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMinPlusOne) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1, signedIntMin, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMinPlusOne) {
      goto ERROR;
    }
    // min+1 == min+1 (all unsigned int) + reversed
    if (__builtin_add_overflow(zero, 1u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != 1u) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1u, zero, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != 1u) {
      goto ERROR;
    }
    // min+1 == min+1 (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMin, 1l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMinPlusOne) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1l, signedLongMin, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMinPlusOne) {
      goto ERROR;
    }
    // min+1 == min+1 (all unsigned long) + reversed
    if (__builtin_add_overflow((unsigned long)zero, 1ul, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != 1ul) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1ul, (unsigned long)zero, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != 1ul) {
      goto ERROR;
    }
    // min+1 == min+1 (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMin, 1ll, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMinPlusOne) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1ll, signedLongLongMin, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMinPlusOne) {
      goto ERROR;
    }
    // min+1 == min+1 (all unsigned long long) + reversed
    if (__builtin_add_overflow((unsigned long long)zero, 1ull, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 1ull) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1ull, (unsigned long long)zero, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 1ull) {
      goto ERROR;
    }

    // min+max == 0 (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMax, signedIntMin, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != 0) {
      goto ERROR;
    }
    if (__builtin_add_overflow(signedIntMin, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != 0) {
      goto ERROR;
    }
    // min+max == max (all unsigned int) + reversed
    if (__builtin_add_overflow(zero, unsignedIntMax, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(unsignedIntMax, zero, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    // min+max == 0 (all signed long) + reversed
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
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(unsignedLongMax, (unsigned long)zero, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    // min+max == 0 (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMin, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      goto ERROR;
    }
    if (__builtin_add_overflow(signedLongLongMax, signedLongLongMin, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      goto ERROR;
    }
    // min+max == max (all unsigned long long) + reversed
    if (__builtin_add_overflow((unsigned long long)zero, unsignedLongLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(unsignedLongLongMax, (unsigned long long)zero, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    // (min+1)+max == 2 (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMax, signedIntMinPlusOne, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != 1) {
      goto ERROR;
    }
    if (__builtin_add_overflow(signedIntMinPlusOne, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != 1) {
      goto ERROR;
    }
    // (min+1)+max == 2 (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMax, signedLongMinPlusOne, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != 1l) {
      goto ERROR;
    }
    if (__builtin_add_overflow(signedLongMinPlusOne, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != 1l) {
      goto ERROR;
    }
    // (min+1)+max == 2 (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMax, signedLongLongMinPlusOne, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 1ll) {
      goto ERROR;
    }
    if (__builtin_add_overflow(signedLongLongMinPlusOne, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 1ll) {
      goto ERROR;
    }
    // (max-1)+1 == max (all signed int) + reversed
    if (__builtin_add_overflow(signedIntMaxMinusOne, 1, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1, signedIntMaxMinusOne, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned int) + reversed
    if (__builtin_add_overflow(unsignedIntMaxMinusOne, 1u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1u, unsignedIntMaxMinusOne, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    // (max-1)+1 == max (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMaxMinusOne, 1l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1l, signedLongMaxMinusOne, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned long) + reversed
    if (__builtin_add_overflow(unsignedLongMaxMinusOne, 1ul, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1ul, unsignedLongMaxMinusOne, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    // (max-1)+1 == max (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMaxMinusOne, 1ll, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1ll, signedLongLongMaxMinusOne, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned long long) + reversed
    if (__builtin_add_overflow(unsignedLongLongMaxMinusOne, 1ull, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1ull, unsignedLongLongMaxMinusOne, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all signed int) + reversed
    if (!__builtin_add_overflow(signedIntMaxMinusOne, 2, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      goto ERROR;
    }

    if (!__builtin_add_overflow(2, signedIntMaxMinusOne, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      goto ERROR;
    }

    // (smax-1)+2 == smax+1 (input signed int, output unsigned int) + reversed
    if (__builtin_add_overflow(signedIntMaxMinusOne, 2, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != signedIntMax+1) {
      goto ERROR;
    }

    if (__builtin_add_overflow(2, signedIntMaxMinusOne, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != signedIntMax+1) {
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned int) + reversed
    if (__builtin_add_overflow(unsignedIntMaxMinusOne, 1u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1u, unsignedIntMaxMinusOne, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    // (max-1)+1 == max (all signed long) + reversed
    if (__builtin_add_overflow(signedLongMaxMinusOne, 1l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1l, signedLongMaxMinusOne, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned long) + reversed
    if (__builtin_add_overflow(unsignedLongMaxMinusOne, 1ul, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1ul, unsignedLongMaxMinusOne, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    // (max-1)+1 == max (all signed long long) + reversed
    if (__builtin_add_overflow(signedLongLongMaxMinusOne, 1ll, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1ll, signedLongLongMaxMinusOne, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      goto ERROR;
    }
    // (max-1)+1 == max (all unsigned long long) + reversed
    if (__builtin_add_overflow(unsignedLongLongMaxMinusOne, 1ull, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(1ull, unsignedLongLongMaxMinusOne, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all unsigned int) + reversed
    if (!__builtin_add_overflow(unsignedIntMaxMinusOne, 2u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + 1u)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(2u, unsignedIntMaxMinusOne, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + 1u)) {
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all signed long) + reversed
    if (!__builtin_add_overflow(signedLongMaxMinusOne, 2l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != LONG_MIN) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(2l, signedLongMaxMinusOne, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != LONG_MIN) {
      goto ERROR;
    }
    // (smax-1)+2 == smax+1 (input signed long, output unsigned long) + reversed
    if (__builtin_add_overflow(signedLongMaxMinusOne, 2l, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != signedLongMax + 1l) {
      goto ERROR;
    }
    if (__builtin_add_overflow(2l, signedLongMaxMinusOne, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != signedLongMax + 1l) {
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all unsigned long) + reversed
    if (!__builtin_add_overflow(unsignedLongMaxMinusOne, 2ul, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMax + 1ul)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(2ul, unsignedLongMaxMinusOne, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMax + 1ul)) {
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all signed long long) + reversed
    if (!__builtin_add_overflow(signedLongLongMaxMinusOne, 2ll, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != LONG_MIN) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(2ll, signedLongLongMaxMinusOne, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != LONG_MIN) {
      goto ERROR;
    }
    // (smax-1)+2 == smax+1 (input signed long long, output unsigned long long) + reversed
    if (__builtin_add_overflow(signedLongLongMaxMinusOne, 2ll, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != signedLongLongMax + 1ll) {
      goto ERROR;
    }
    if (__builtin_add_overflow(2ll, signedLongLongMaxMinusOne, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != signedLongLongMax + 1ll) {
      goto ERROR;
    }
    // (max-1)+2 == max+1 overflow (all unsigned long long) + reversed
    if (!__builtin_add_overflow(unsignedLongLongMaxMinusOne, 2ull, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + 1ull)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(2ull, unsignedLongLongMaxMinusOne, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + 1ull)) {
      goto ERROR;
    }
    // (max-1)+max == max overflow (all signed int) + reversed
    if (!__builtin_add_overflow(signedIntMaxMinusOne, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -3) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedIntMax, signedIntMaxMinusOne, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -3) {
      goto ERROR;
    }
    // (max-1)+max == max overflow (all unsigned int) + reversed
    if (!__builtin_add_overflow(unsignedIntMaxMinusOne, unsignedIntMax, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMaxMinusOne + unsignedIntMax)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedIntMax, unsignedIntMaxMinusOne, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMaxMinusOne + unsignedIntMax)) {
      goto ERROR;
    }
    // (max-1)+max == max overflow (all signed long) + reversed
    if (!__builtin_add_overflow(signedLongMaxMinusOne, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -3l) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongMax, signedLongMaxMinusOne, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -3l) {
      goto ERROR;
    }
    // (max-1)+max == max overflow (all unsigned long) + reversed
    if (!__builtin_add_overflow(unsignedLongMaxMinusOne, unsignedLongMax, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMaxMinusOne + unsignedLongMax)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMaxMinusOne, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMaxMinusOne + unsignedLongMax)) {
      goto ERROR;
    }
    // (max-1)+max == max overflow (all signed long long) + reversed
    if (!__builtin_add_overflow(signedLongLongMaxMinusOne, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -3ll) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMaxMinusOne, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -3ll) {
      goto ERROR;
    }
    // (max-1)+max == max overflow (all unsigned long long) + reversed
    if (!__builtin_add_overflow(unsignedLongLongMaxMinusOne, unsignedLongLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMaxMinusOne + unsignedLongLongMax)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMaxMinusOne, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMaxMinusOne + unsignedLongLongMax)) {
      goto ERROR;
    }
    // max+max == max overflow (all signed int)
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    // max+max == max overflow (all unsigned int)
    if (!__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + unsignedIntMax)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != (unsignedIntMax + unsignedIntMax)) {
      goto ERROR;
    }
    // max+max == max overflow (all signed long)
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    // max+max == max overflow (all unsigned long)
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMax + unsignedLongMax)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != (unsignedLongMax + unsignedLongMax)) {
      goto ERROR;
    }
    // max+max == max overflow (all signed long long)
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    // max+max == max overflow (all unsigned long long)
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + unsignedLongLongMax)) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != (unsignedLongLongMax + unsignedLongLongMax)) {
      goto ERROR;
    }
    // max+0 == max (input signed int, output signed int)
    if (__builtin_add_overflow(signedIntMax, zero, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    if (__builtin_add_overflow(zero, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    // max+0 == max overflow (input unsigned int, output signed int)
    if (!__builtin_add_overflow(unsignedIntMax, 0, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(0, unsignedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    // max+0 == max overflow (input signed long, output signed int)
    if (!__builtin_add_overflow(signedLongMax, 0, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(0, signedLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    // max+0 == max overflow (input unsigned long, output signed int)
    if (!__builtin_add_overflow(unsignedLongMax, 0ul, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(0ul, unsignedLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    // max+0 == max overflow (input signed long long, output signed int)
    if (!__builtin_add_overflow(signedLongLongMax, 0ll, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(0ll, signedLongLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    // max+0 == max overflow (input unsigned long long, output signed int)
    if (!__builtin_add_overflow(unsignedLongLongMax, 0ull, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(0ull, unsignedLongLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -1) {
      goto ERROR;
    }
    // max+max == max overflow (input signed int, output signed int)
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    // max+max == max overflow (input unsigned int, output signed int)
    if (!__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    // max+max == max overflow (input signed long, output signed int)
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long, output signed int)
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    // max+max == max overflow (input signed long long, output signed int)
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long long, output signed int)
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    // max+max == 2*max signed int (input signed int, output signed long)
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    // max+max == 2*max unsigned int (input unsigned int, output signed long)
    if (__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != 8589934590l) { // 2 * 4294967295
      goto ERROR;
    }
    if (__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != 8589934590l) {
      goto ERROR;
    }
    // max+max == max overflow (input signed long, output signed long)
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long, output signed long)
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    // max+max == max overflow (input signed long long, output signed long)
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long long, output signed long)
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    // max+max == 2*max signed int (input signed int, output signed long long)
    if (__builtin_add_overflow(signedIntMax, signedIntMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 4294967294L) {
      goto ERROR;
    }
    // max+max == 2*max unsigned int (input unsigned int, output signed long long)
    if (__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 8589934590ll) { // 2 * 4294967295
      goto ERROR;
    }
    if (__builtin_add_overflow(unsignedIntMax, unsignedIntMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 8589934590ll) {
      goto ERROR;
    }
    // max+max == max overflow (input signed long, output signed long long)
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongMax, signedLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long, output signed long long)
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    // max+max == max overflow (input signed long long, output signed long long)
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long long, output signed long long)
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    if (!__builtin_add_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
    // max+max == 2*max signed long (input signed long, output unsigned long long)
    if (__builtin_add_overflow(signedLongMax, signedLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 18446744073709551614ULL) {
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long, output unsigned long long)
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax - 1ULL) {
      goto ERROR;
    }

  }
  // __builtin_sadd_overflow, signed int
  {
    int sIntResult;
    // 0+0 == 0
    if (__builtin_sadd_overflow(zero, zero, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != zero) {
      goto ERROR;
    }
    // max+0 == max (all signed int) + reversed
    if (__builtin_sadd_overflow(signedIntMax, zero, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    if (__builtin_sadd_overflow(zero, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    // max+1 == overflow (all signed int) + reversed
    if (!__builtin_sadd_overflow(signedIntMax, 1, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      goto ERROR;
    }
    if (!__builtin_sadd_overflow(1, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != INT_MIN) {
      goto ERROR;
    }
    // min+1 == min+1 (all signed int) + reversed
    if (__builtin_sadd_overflow(signedIntMin, 1, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMinPlusOne) {
      goto ERROR;
    }
    if (__builtin_sadd_overflow(1, signedIntMin, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMinPlusOne) {
      goto ERROR;
    }
    // INT_MIN-1 == overflow (all signed int)
    if (!__builtin_sadd_overflow(INT_MIN, -1, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != INT_MAX) {
      goto ERROR;
    }
  }
  // __builtin_saddl_overflow, signed long
  {
   long sLongResult;
   if (__builtin_saddl_overflow(0l, 0l, &sLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (sLongResult != 0l) {
    assert(0);
    goto ERROR;
   }
   if (__builtin_saddl_overflow(signedLongMax, 0l, &sLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (sLongResult != signedLongMax) {
    assert(0);
    goto ERROR;
   }
   if (!__builtin_saddl_overflow(signedLongMax, 1l, &sLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (sLongResult != LONG_MIN) {
    assert(0);
    goto ERROR;
   }
   if (__builtin_saddl_overflow(signedLongMin, 1l, &sLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (sLongResult != signedLongMinPlusOne) {
    assert(0);
    goto ERROR;
   }
  }

  // __builtin_saddll_overflow, signed long long
  {
   long long sLongLongResult;
   if (__builtin_saddll_overflow(0ll, 0ll, &sLongLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (sLongLongResult != 0ll) {
    assert(0);
    goto ERROR;
   }
   if (__builtin_saddll_overflow(signedLongLongMax, 0ll, &sLongLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (sLongLongResult != signedLongLongMax) {
    assert(0);
    goto ERROR;
   }
   if (!__builtin_saddll_overflow(signedLongLongMax, 1ll, &sLongLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (sLongLongResult != LONG_MIN) {
    assert(0);
    goto ERROR;
   }
   if (__builtin_saddll_overflow(signedLongLongMin, 1ll, &sLongLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (sLongLongResult != signedLongLongMinPlusOne) {
    assert(0);
    goto ERROR;
   }
  }

  // __builtin_uadd_overflow, unsigned int
  {
   unsigned int uIntResult;
   if (__builtin_uadd_overflow(0u, 0u, &uIntResult)) {
    assert(0);
    goto ERROR;
   }
   if (uIntResult != 0u) {
    assert(0);
    goto ERROR;
   }
   if (__builtin_uadd_overflow(unsignedIntMax, 0u, &uIntResult)) {
    assert(0);
    goto ERROR;
   }
   if (uIntResult != unsignedIntMax) {
    assert(0);
    goto ERROR;
   }
   if (!__builtin_uadd_overflow(unsignedIntMax, 1u, &uIntResult)) {
    assert(0);
    goto ERROR;
   }
   if (uIntResult != unsignedIntMax + 1u) {
    assert(0);
    goto ERROR;
   }
   if (!__builtin_uadd_overflow(1u, unsignedIntMax, &uIntResult)) {
    assert(0);
    goto ERROR;
   }
   if (uIntResult != unsignedIntMax + 1u) {
    assert(0);
    goto ERROR;
   }
  }

  // __builtin_uaddl_overflow, unsigned long
  {
   unsigned long uLongResult;
   if (__builtin_uaddl_overflow(0ul, 0ul, &uLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (uLongResult != 0ul) {
    assert(0);
    goto ERROR;
   }
   if (__builtin_uaddl_overflow(unsignedLongMax, 0ul, &uLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (uLongResult != unsignedLongMax) {
    assert(0);
    goto ERROR;
   }
   if (!__builtin_uaddl_overflow(unsignedLongMax, 1ul, &uLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (uLongResult != unsignedLongMax + 1ul) {
    assert(0);
    goto ERROR;
   }
   if (!__builtin_uaddl_overflow(1ul, unsignedLongMax, &uLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (uLongResult != unsignedLongMax + 1ul) {
    assert(0);
    goto ERROR;
   }
  }

  // __builtin_uaddll_overflow, unsigned long long
  {
   unsigned long long uLongLongResult;
   if (__builtin_uaddll_overflow(0ull, 0ull, &uLongLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (uLongLongResult != 0ull) {
    assert(0);
    goto ERROR;
   }
   if (__builtin_uaddll_overflow(unsignedLongLongMax, 0ull, &uLongLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (uLongLongResult != unsignedLongLongMax) {
    assert(0);
    goto ERROR;
   }
   if (!__builtin_uaddll_overflow(unsignedLongLongMax, 1ull, &uLongLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (uLongLongResult != unsignedLongLongMax + 1ull) {
    assert(0);
    goto ERROR;
   }
   if (!__builtin_uaddll_overflow(1ull, unsignedLongLongMax, &uLongLongResult)) {
    assert(0);
    goto ERROR;
   }
   if (uLongLongResult != unsignedLongLongMax + 1ull) {
    assert(0);
    goto ERROR;
   }
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
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_sub_overflow(signedLongLongMax, 0, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_sub_overflow(0, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -signedLongLongMax) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_sub_overflow(0, 0, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_sub_overflow(signedLongLongMin, -1, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -9223372036854775806) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }
  }
// __builtin_ssub_overflow
{
  int sIntResult;
  if (__builtin_ssub_overflow(zero, zero, &sIntResult)) {
    assert(0);
    goto ERROR;
  }
  if (sIntResult != 0) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_ssub_overflow(signedIntMax, zero, &sIntResult)) {
    assert(0);
    goto ERROR;
  }
  if (sIntResult != signedIntMax) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_ssub_overflow(signedIntMin, zero, &sIntResult)) {
    assert(0);
    goto ERROR;
  }
  if (sIntResult != signedIntMin) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_ssub_overflow(signedIntMin, 1, &sIntResult)) {
    assert(0);
    goto ERROR;
  }
  if (sIntResult != INT_MIN) {
    assert(0);
    goto ERROR;
  }
  if (!__builtin_ssub_overflow(signedIntMin, 2, &sIntResult)) {
    assert(0);
    goto ERROR;
  }
  if (sIntResult != signedIntMax) {
    assert(0);
    goto ERROR;
  }
}

// __builtin_ssubl_overflow
{
  long sLongResult;
  if (__builtin_ssubl_overflow(0l, 0l, &sLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongResult != 0l) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_ssubl_overflow(signedLongMax, 0l, &sLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongResult != signedLongMax) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_ssubl_overflow(signedLongMin, 0l, &sLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongResult != signedLongMin) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_ssubl_overflow(signedLongMin, 1l, &sLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongResult != LONG_MIN) {
    assert(0);
    goto ERROR;
  }
  if (!__builtin_ssubl_overflow(signedLongMin, 2l, &sLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongResult != signedLongMax) {
    assert(0);
    goto ERROR;
  }
}

// __builtin_ssubll_overflow
{
  long long sLongLongResult;
  if (__builtin_ssubll_overflow(0ll, 0ll, &sLongLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongLongResult != 0ll) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_ssubll_overflow(signedLongLongMax, 0ll, &sLongLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongLongResult != signedLongLongMax) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_ssubll_overflow(signedLongLongMin, 0ll, &sLongLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongLongResult != signedLongLongMin) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_ssubll_overflow(signedLongLongMin, 1ll, &sLongLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongLongResult != LONG_MIN) {
    assert(0);
    goto ERROR;
  }
  if (!__builtin_ssubll_overflow(signedLongLongMin, 2ll, &sLongLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (sLongLongResult != signedLongLongMax) {
    assert(0);
    goto ERROR;
  }
}

// __builtin_usub_overflow
{
  unsigned int uIntResult;
  if (__builtin_usub_overflow(0u, 0u, &uIntResult)) {
    assert(0);
    goto ERROR;
  }
  if (uIntResult != 0u) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_usub_overflow(unsignedIntMax, 0u, &uIntResult)) {
    assert(0);
    goto ERROR;
  }
  if (uIntResult != unsignedIntMax) {
    assert(0);
    goto ERROR;
  }
  if (!__builtin_usub_overflow(0u, 1u, &uIntResult)) {
    assert(0);
    goto ERROR;
  }
  if (uIntResult != unsignedIntMax) {
    assert(0);
    goto ERROR;
  }
}

// __builtin_usubl_overflow
{
  unsigned long uLongResult;
  if (__builtin_usubl_overflow(0ul, 0ul, &uLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (uLongResult != 0ul) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_usubl_overflow(unsignedLongMax, 0ul, &uLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (uLongResult != unsignedLongMax) {
    assert(0);
    goto ERROR;
  }
  if (!__builtin_usubl_overflow(0ul, 1ul, &uLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (uLongResult != unsignedLongMax) {
    assert(0);
    goto ERROR;
  }
}

// __builtin_usubll_overflow
{
  unsigned long long uLongLongResult;
  if (__builtin_usubll_overflow(0ull, 0ull, &uLongLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (uLongLongResult != 0ull) {
    assert(0);
    goto ERROR;
  }
  if (__builtin_usubll_overflow(unsignedLongLongMax, 0ull, &uLongLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (uLongLongResult != unsignedLongLongMax) {
    assert(0);
    goto ERROR;
  }
  if (!__builtin_usubll_overflow(0ull, 1ull, &uLongLongResult)) {
    assert(0);
    goto ERROR;
  }
  if (uLongLongResult != unsignedLongLongMax) {
    assert(0);
    goto ERROR;
  }
}

  // __builtin_mul_overflow(), types can be chosen freely
  {
    int sIntResult;
    long sLongResult;
    long long sLongLongResult;
    unsigned int uIntResult;
    unsigned long uLongResult;
    unsigned long long uLongLongResult;

    // 0*0 == 0 (all signed int)
    if (__builtin_mul_overflow(zero, zero, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != zero) {
      goto ERROR;
    }
    // 0*0 == 0 (all unsigned int)
    if (__builtin_mul_overflow((unsigned int)zero, (unsigned int)zero, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != 0u) {
      goto ERROR;
    }
    // 0*0 == 0 (all signed long)
    if (__builtin_mul_overflow((long)zero, (long)zero, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != 0l) {
      goto ERROR;
    }
    // 0*0 == 0 (all unsigned long)
    if (__builtin_mul_overflow((unsigned long)zero, (unsigned long)zero, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != 0ul) {
      goto ERROR;
    }
    // max*0 == 0 (all signed int) + reversed
    if (__builtin_mul_overflow(signedIntMax, zero, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != 0) {
      goto ERROR;
    }
    if (__builtin_mul_overflow(zero, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != 0) {
      goto ERROR;
    }
    // max*0 == 0 (all unsigned int) + reversed
    if (__builtin_mul_overflow(unsignedIntMax, (unsigned int)zero, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != 0u) {
      goto ERROR;
    }
    if (__builtin_mul_overflow((unsigned int)zero, unsignedIntMax, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != 0u) {
      goto ERROR;
    }
    // max*0 == 0 (all signed long) + reversed
    if (__builtin_mul_overflow(signedLongMax, (long)zero, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != 0l) {
      goto ERROR;
    }
    if (__builtin_mul_overflow((long)zero, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != 0l) {
      goto ERROR;
    }
    // max*0 == 0 (all unsigned long) + reversed
    if (__builtin_mul_overflow(unsignedLongMax, (unsigned long)zero, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != 0ul) {
      goto ERROR;
    }
    if (__builtin_mul_overflow((unsigned long)zero, unsignedLongMax, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != 0ul) {
      goto ERROR;
    }
    // max*1 == max (all signed int) + reversed
    if (__builtin_mul_overflow(signedIntMax, 1, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    if (__builtin_mul_overflow(1, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    // max*1 == max (all unsigned int) + reversed
    if (__builtin_mul_overflow(unsignedIntMax, 1u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    if (__builtin_mul_overflow(1u, unsignedIntMax, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    // max*1 == max (all signed long) + reversed
    if (__builtin_mul_overflow(signedLongMax, 1l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      goto ERROR;
    }
    if (__builtin_mul_overflow(1l, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      goto ERROR;
    }
    // max*1 == max (all unsigned long) + reversed
    if (__builtin_mul_overflow(unsignedLongMax, 1ul, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    if (__builtin_mul_overflow(1ul, unsignedLongMax, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    // max*2 == overflow (all signed int)
    if (!__builtin_mul_overflow(signedIntMax, 2, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    if (!__builtin_mul_overflow(2, signedIntMax, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
    // max*2 == overflow (all unsigned int)
    if (!__builtin_mul_overflow(unsignedIntMax, 2u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMaxMinusOne) {
      goto ERROR;
    }
    if (!__builtin_mul_overflow(2u, unsignedIntMax, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMaxMinusOne) {
      goto ERROR;
    }
    // max*2 == overflow (all signed long)
    if (!__builtin_mul_overflow(signedLongMax, 2l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    if (!__builtin_mul_overflow(2l, signedLongMax, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
    // max*2 == overflow (all unsigned long)
    if (!__builtin_mul_overflow(unsignedLongMax, 2ul, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMaxMinusOne) {
      goto ERROR;
    }
    if (!__builtin_mul_overflow(2ul, unsignedLongMax, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMaxMinusOne) {
      goto ERROR;
    }

    // long or long long into long/long long
    if (!__builtin_mul_overflow(signedLongLongMax, signedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 1) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedLongLongMax, 0, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(0, longLongArray[0], &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedLongLongMax, 1, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

        if (__builtin_mul_overflow(1, getLongLongMax(), &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }


    // long or long long into unsigned long/unsigned long long
    if (!__builtin_mul_overflow(signedLongLongMax, signedLongLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 1) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedLongLongMax, 0, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 0) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedLongLongMax, 1, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != signedLongLongMax) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }


    // unsigned long or unsigned long long into long/long long
    if (!__builtin_mul_overflow(unsignedLongLongMax, unsignedLongLongMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 1) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedLongLongMax, 0, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 0) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(unsignedLongLongMax, 1, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -1) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }


    // unsigned long/unsigned long long into unsigned long/unsigned long long
    if (!__builtin_mul_overflow(unsignedLongLongMax, unsignedLongLongMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 1) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedLongLongMax, 0, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 0) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedLongLongMax, 1, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    // Cases in which the multiplication fits into the larger target type
    // int max + int max == int max² in signed/unsigned long/long long
    if (__builtin_mul_overflow(signedIntMax, signedIntMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 4611686014132420609LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedIntMax, signedIntMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 4611686014132420609LL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    // s int max * u int max into signed/unsigned long long
    if (__builtin_mul_overflow(signedIntMax, unsignedIntMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 9223372030412324865LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedIntMax, unsignedIntMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 9223372030412324865LL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedIntMax, signedIntMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 9223372030412324865LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(unsignedIntMax, signedIntMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 9223372030412324865LL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    // s int max * s int min into signed/unsigned long long
    if (__builtin_mul_overflow(signedIntMax, signedIntMin, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -4611686014132420609LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(signedIntMax, signedIntMin, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 13835058059577131007ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (__builtin_mul_overflow(signedIntMin, signedIntMax, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -4611686014132420609LL) {
      printf("sLongLongResult: %lld", sLongLongResult);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(signedIntMin, signedIntMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 13835058059577131007ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    // Test that we can insert values also via pointer-deref, arrays, functions, structs etc.
    if (!__builtin_mul_overflow(*getIntMinPointer(), getIntMax(), &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 13835058059577131007ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(intArray[1], (*intArrayPtr)[0], &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 13835058061724614654ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(*intArray, (*intArrayPtr)[1], &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 13835058061724614654ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(intStruct.intMin, intStructPtr->intMax, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 13835058061724614654ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }

    if (!__builtin_mul_overflow(intStruct.intMax, (*intStructPtr).intMin, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 13835058061724614654ULL) {
      printf("uLongLongResult: %llu", uLongLongResult);
      goto ERROR;
    }


  }
  // __builtin_smul_overflow
  {
    int sIntResult;
    if (__builtin_smul_overflow(0, 0, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != 0) {
      goto ERROR;
    }
    if (__builtin_smul_overflow(signedIntMax, 1, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      goto ERROR;
    }
    if (!__builtin_smul_overflow(signedIntMax, 2, &sIntResult)) {
      goto ERROR;
    }
    if (sIntResult != -2) {
      goto ERROR;
    }
  }

  // __builtin_smull_overflow
  {
    long sLongResult;
    if (__builtin_smull_overflow(0l, 0l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != 0l) {
      goto ERROR;
    }
    if (__builtin_smull_overflow(signedLongMax, 1l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != signedLongMax) {
      goto ERROR;
    }
    if (!__builtin_smull_overflow(signedLongMax, 2l, &sLongResult)) {
      goto ERROR;
    }
    if (sLongResult != -2l) {
      goto ERROR;
    }
  }

  // __builtin_smulll_overflow
  {
    long long sLongLongResult;
    if (__builtin_smulll_overflow(0ll, 0ll, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != 0ll) {
      goto ERROR;
    }
    if (__builtin_smulll_overflow(signedLongLongMax, 1ll, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != signedLongLongMax) {
      goto ERROR;
    }
    if (!__builtin_smulll_overflow(signedLongLongMax, 2ll, &sLongLongResult)) {
      goto ERROR;
    }
    if (sLongLongResult != -2ll) {
      goto ERROR;
    }
  }

  // __builtin_umul_overflow
  {
    unsigned int uIntResult;
    if (__builtin_umul_overflow(0u, 0u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != 0u) {
      goto ERROR;
    }
    if (__builtin_umul_overflow(unsignedIntMax, 1u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    if (!__builtin_umul_overflow(unsignedIntMax, 2u, &uIntResult)) {
      goto ERROR;
    }
    if (uIntResult != unsignedIntMaxMinusOne) {
      goto ERROR;
    }
  }

  // __builtin_umull_overflow
  {
    unsigned long uLongResult;
    if (__builtin_umull_overflow(0ul, 0ul, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != 0ul) {
      goto ERROR;
    }
    if (__builtin_umull_overflow(unsignedLongMax, 1ul, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    if (!__builtin_umull_overflow(unsignedLongMax, 2ul, &uLongResult)) {
      goto ERROR;
    }
    if (uLongResult != unsignedLongMaxMinusOne) {
      goto ERROR;
    }
  }

  // __builtin_umulll_overflow
  {
    unsigned long long uLongLongResult;
    if (__builtin_umulll_overflow(0ull, 0ull, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != 0ull) {
      goto ERROR;
    }
    if (__builtin_umulll_overflow(unsignedLongLongMax, 1ull, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    if (!__builtin_umulll_overflow(unsignedLongLongMax, 2ull, &uLongLongResult)) {
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMaxMinusOne) {
      goto ERROR;
    }
  }


  // ############ Methods checking only for overflow, not returning the arithmetic result actual value 0 ############
  {
    {
      // __builtin_add_overflow_p(), types can be chosen freely
      if (!__builtin_add_overflow_p(signedIntMax, 1, (int)0)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(signedIntMaxMinusOne, 1, (int)0)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_add_overflow_p(unsignedIntMax, 1u, (unsigned int)0)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(unsignedIntMaxMinusOne, 1u, (unsigned int)0)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(0, 0, (int)0)) {
        assert(0);
        goto ERROR;
      }
    }

    {
      // __builtin_sub_overflow_p(), types can be chosen freely
      if (__builtin_sub_overflow_p(signedIntMax, signedIntMax, (int)0)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(signedIntMin, 1, (int)0)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p(signedIntMin, 2, (int)0)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(unsignedIntMax, unsignedIntMax, (unsigned int)0)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p((unsigned int)0, 1u, (unsigned int)0)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(0, 0, (int)0)) {
        assert(0);
        goto ERROR;
      }
    }

    {
      // __builtin_mul_overflow_p(), types can be chosen freely
      if (__builtin_mul_overflow_p(0, signedIntMax, (int)0)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(signedIntMin, 1, (int)0)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p(signedIntMin, 2, (int)0)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_mul_overflow_p(1u, unsignedIntMax, (unsigned int)0)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_mul_overflow_p(unsignedIntMax, 2u, (unsigned int)0)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_mul_overflow_p(1, 1, (int)0)) {
        assert(0);
        goto ERROR;
      }
    }
  }
  // ############ Methods checking only for overflow, not returning the arithmetic result actual value 5 ############
  {
    {
      // __builtin_add_overflow_p(), types can be chosen freely
      if (!__builtin_add_overflow_p(signedIntMax, 1, (int)5)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(signedIntMaxMinusOne, 1, (int)5)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_add_overflow_p(unsignedIntMax, 1u, (unsigned int)5)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(unsignedIntMaxMinusOne, 1u, (unsigned int)5)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(0, 0, (int)5)) {
        assert(0);
        goto ERROR;
      }
    }

    {
      // __builtin_sub_overflow_p(), types can be chosen freely
      if (__builtin_sub_overflow_p(signedIntMax, signedIntMax, (int)5)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(signedIntMin, 1, (int)5)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p(signedIntMin, 2, (int)5)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(unsignedIntMax, unsignedIntMax, (unsigned int)5)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p((unsigned int)0, 1u, (unsigned int)5)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(0, 0, (int)5)) {
        assert(0);
        goto ERROR;
      }
    }

    {
      // __builtin_mul_overflow_p(), types can be chosen freely
      if (__builtin_mul_overflow_p(0, signedIntMax, (int)5)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(signedIntMin, 1, (int)5)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p(signedIntMin, 2, (int)5)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_mul_overflow_p(1u, unsignedIntMax, (unsigned int)5)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_mul_overflow_p(unsignedIntMax, 2u, (unsigned int)5)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_mul_overflow_p(1, 1, (int)5)) {
        assert(0);
        goto ERROR;
      }
    }
  }

  // ############ Methods checking only for overflow, not returning the arithmetic result type variable ############
  {
    {
      int intVar = 0;
      unsigned int uIntVar = 0;
      // __builtin_add_overflow_p(), types can be chosen freely
      if (!__builtin_add_overflow_p(signedIntMax, 1, intVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(signedIntMaxMinusOne, 1, intVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_add_overflow_p(unsignedIntMax, 1u, uIntVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(unsignedIntMaxMinusOne, 1u, uIntVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(0, 0, intVar)) {
        assert(0);
        goto ERROR;
      }
    }

    {
      int intVar = 0;
      unsigned int uIntVar = 0;
      // __builtin_sub_overflow_p(), types can be chosen freely
      if (__builtin_sub_overflow_p(signedIntMax, signedIntMax, intVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(signedIntMin, 1, intVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p(signedIntMin, 2, intVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(unsignedIntMax, unsignedIntMax, uIntVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p((unsigned int)0, 1u, uIntVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(0, 0, intVar)) {
        assert(0);
        goto ERROR;
      }
    }

    {
      int intVar = 0;
      unsigned int uIntVar = 0;
      // __builtin_mul_overflow_p(), types can be chosen freely
      if (__builtin_mul_overflow_p(0, signedIntMax, intVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(signedIntMin, 1, intVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p(signedIntMin, 2, intVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_mul_overflow_p(1u, unsignedIntMax, uIntVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_mul_overflow_p(unsignedIntMax, 2u, uIntVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_mul_overflow_p(1, 1, intVar)) {
        assert(0);
        goto ERROR;
      }
    }
  }

  // ############ Methods checking only for overflow, not returning the arithmetic result type cast ############
  {
    {
      long longVar = 0;
      unsigned long uLongVar = 0;
      // __builtin_add_overflow_p(), types can be chosen freely
      if (!__builtin_add_overflow_p(signedIntMax, 1, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(signedIntMaxMinusOne, 1, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_add_overflow_p(unsignedIntMax, 1u, (unsigned int) uLongVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(unsignedIntMaxMinusOne, 1u, (unsigned int) uLongVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_add_overflow_p(0, 0, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
    }

    {
      long longVar = 0;
      unsigned long uLongVar = 0;
      // __builtin_sub_overflow_p(), types can be chosen freely
      if (__builtin_sub_overflow_p(signedIntMax, signedIntMax, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(signedIntMin, 1, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p(signedIntMin, 2, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(unsignedIntMax, unsignedIntMax, (unsigned int) uLongVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p((unsigned int)0, 1u, (unsigned int) uLongVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(0, 0, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
    }

    {
      long longVar = 0;
      unsigned long uLongVar = 0;
      // __builtin_mul_overflow_p(), types can be chosen freely
      if (__builtin_mul_overflow_p(0, signedIntMax, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_sub_overflow_p(signedIntMin, 1, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_sub_overflow_p(signedIntMin, 2, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_mul_overflow_p(1u, unsignedIntMax, (unsigned int) uLongVar)) {
        assert(0);
        goto ERROR;
      }
      if (!__builtin_mul_overflow_p(unsignedIntMax, 2u, (unsigned int) uLongVar)) {
        assert(0);
        goto ERROR;
      }
      if (__builtin_mul_overflow_p(1, 1, (int) longVar)) {
        assert(0);
        goto ERROR;
      }
    }
  }


  // ############ Methods returning the arithmetic result directly ############
  // (They also have a third argument that is added/subtracted, and return the boolean overflow result using a side effect)
  {
    // __builtin_addc(), unsigned int
    unsigned int carryin;
    unsigned int carryout;
    unsigned int uIntResult;
    carryin = 0u;
    uIntResult = __builtin_addc(0u, 0u, carryin, &carryout);
    if (uIntResult != 0u) {
      goto ERROR;
    }
    if (carryout != 0u) {
      goto ERROR;
    }
    carryin = 0u;
    uIntResult = __builtin_addc(unsignedIntMax, 0u, carryin, &carryout);
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    if (carryout != 0u) {
      goto ERROR;
    }
    carryin = 0u;
    uIntResult = __builtin_addc(unsignedIntMax, 1u, carryin, &carryout);
    if (uIntResult != 0u) {
      goto ERROR;
    }
    if (carryout != 1u) {
      goto ERROR;
    }
    carryin = 0u;
    uIntResult = __builtin_addc(unsignedIntMax, unsignedIntMax, carryin, &carryout);
    if (uIntResult != unsignedIntMax - 1u) {
      goto ERROR;
    }
    if (carryout != 1u) {
      goto ERROR;
    }
  }

  {
    // __builtin_addcl(), unsigned long
    unsigned long carryin;
    unsigned long carryout;
    unsigned long uLongResult;
    carryin = 0ul;
    uLongResult = __builtin_addcl(0ul, 0ul, carryin, &carryout);
    if (uLongResult != 0ul) {
      goto ERROR;
    }
    if (carryout != 0ul) {
      goto ERROR;
    }
    carryin = 0ul;
    uLongResult = __builtin_addcl(unsignedLongMax, 0ul, carryin, &carryout);
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    if (carryout != 0ul) {
      goto ERROR;
    }
    carryin = 0ul;
    uLongResult = __builtin_addcl(unsignedLongMax, 1ul, carryin, &carryout);
    if (uLongResult != 0ul) {
      goto ERROR;
    }
    if (carryout != 1ul) {
      goto ERROR;
    }
    carryin = 0ul;
    uLongResult = __builtin_addcl(unsignedLongMax, unsignedLongMax, carryin, &carryout);
    if (uLongResult != unsignedLongMax - 1ul) {
      goto ERROR;
    }
    if (carryout != 1ul) {
      goto ERROR;
    }
  }

  {
    // __builtin_addcll(), unsigned long long
    unsigned long long carryin;
    unsigned long long carryout;
    unsigned long long uLongLongResult;
    carryin = 0ull;
    uLongLongResult = __builtin_addcll(0ull, 0ull, carryin, &carryout);
    if (uLongLongResult != 0ull) {
      goto ERROR;
    }
    if (carryout != 0ull) {
      goto ERROR;
    }
    carryin = 0ull;
    uLongLongResult = __builtin_addcll(unsignedLongLongMax, 0ull, carryin, &carryout);
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    if (carryout != 0ull) {
      goto ERROR;
    }
    carryin = 0ull;
    uLongLongResult = __builtin_addcll(unsignedLongLongMax, 1ull, carryin, &carryout);
    if (uLongLongResult != 0ull) {
      goto ERROR;
    }
    if (carryout != 1ull) {
      goto ERROR;
    }
    carryin = 0ull;
    uLongLongResult = __builtin_addcll(unsignedLongLongMax, unsignedLongLongMax, carryin, &carryout);
    if (uLongLongResult != unsignedLongLongMax - 1ull) {
      goto ERROR;
    }
    if (carryout != 1ull) {
      goto ERROR;
    }
  }

  {
    // __builtin_subc(), unsigned int
    unsigned int carryin;
    unsigned int carryout;
    unsigned int uIntResult;
    carryin = 0u;
    uIntResult = __builtin_subc(5u, 3u, carryin, &carryout);
    if (uIntResult != 2u) {
      goto ERROR;
    }
    if (carryout != 0u) {
      goto ERROR;
    }
    carryin = 1u;
    uIntResult = __builtin_subc(5u, 3u, carryin, &carryout);
    if (uIntResult != 1u) {
      goto ERROR;
    }
    if (carryout != 0u) {
      goto ERROR;
    }
    carryin = 0u;
    uIntResult = __builtin_subc(0u, 0u, carryin, &carryout);
    if (uIntResult != 0u) {
      goto ERROR;
    }
    if (carryout != 0u) {
      goto ERROR;
    }
    carryin = 0u;
    uIntResult = __builtin_subc(0u, 1u, carryin, &carryout);
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    if (carryout != 1u) {
      goto ERROR;
    }
    carryin = 1u;
    uIntResult = __builtin_subc(0u, 0u, carryin, &carryout);
    if (uIntResult != unsignedIntMax) {
      goto ERROR;
    }
    if (carryout != 1u) {
      goto ERROR;
    }
  }

  {
    // __builtin_subcl(), unsigned long
    unsigned long carryin;
    unsigned long carryout;
    unsigned long uLongResult;
    carryin = 0ul;
    uLongResult = __builtin_subcl(5ul, 3ul, carryin, &carryout);
    if (uLongResult != 2ul) {
      goto ERROR;
    }
    if (carryout != 0ul) {
      goto ERROR;
    }
    carryin = 1ul;
    uLongResult = __builtin_subcl(5ul, 3ul, carryin, &carryout);
    if (uLongResult != 1ul) {
      goto ERROR;
    }
    if (carryout != 0ul) {
      goto ERROR;
    }
    carryin = 0ul;
    uLongResult = __builtin_subcl(0ul, 0ul, carryin, &carryout);
    if (uLongResult != 0ul) {
      goto ERROR;
    }
    if (carryout != 0ul) {
      goto ERROR;
    }
    carryin = 0ul;
    uLongResult = __builtin_subcl(0ul, 1ul, carryin, &carryout);
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    if (carryout != 1ul) {
      goto ERROR;
    }
    carryin = 1ul;
    uLongResult = __builtin_subcl(0ul, 0ul, carryin, &carryout);
    if (uLongResult != unsignedLongMax) {
      goto ERROR;
    }
    if (carryout != 1ul) {
      goto ERROR;
    }
  }

  {
    // __builtin_subcll(), unsigned long long
    unsigned long long carryin;
    unsigned long long carryout;
    unsigned long long uLongLongResult;
    carryin = 0ull;
    uLongLongResult = __builtin_subcll(5ull, 3ull, carryin, &carryout);
    if (uLongLongResult != 2ull) {
      goto ERROR;
    }
    if (carryout != 0ull) {
      goto ERROR;
    }
    carryin = 1ull;
    uLongLongResult = __builtin_subcll(5ull, 3ull, carryin, &carryout);
    if (uLongLongResult != 1ull) {
      goto ERROR;
    }
    if (carryout != 0ull) {
      goto ERROR;
    }
    carryin = 0ull;
    uLongLongResult = __builtin_subcll(0ull, 0ull, carryin, &carryout);
    if (uLongLongResult != 0ull) {
      goto ERROR;
    }
    if (carryout != 0ull) {
      goto ERROR;
    }
    carryin = 0ull;
    uLongLongResult = __builtin_subcll(0ull, 1ull, carryin, &carryout);
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    if (carryout != 1ull) {
      goto ERROR;
    }
    carryin = 1ull;
    uLongLongResult = __builtin_subcll(0ull, 0ull, carryin, &carryout);
    if (uLongLongResult != unsignedLongLongMax) {
      goto ERROR;
    }
    if (carryout != 1ull) {
      goto ERROR;
    }
  }

  return 0; // Expected

  ERROR:
  printf("Error label reached\n"); // Debug for when you compile the program, but does not hurt in verification.
  return 1;
}
