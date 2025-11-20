// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>
#include <assert.h>

int zero = 0;

// Int
int signedIntMax = 2147483647;
int signedIntMaxMinusOne = 2147483646;
int signedIntMin = -2147483646;
int signedIntMinPlusOne = -2147483645;
int signedIntMaxHalfRoundedUp = 1073741824;
int signedIntMaxHalfRoundedDown = 1073741823;

unsigned int unsignedIntMax = 4294967295u;
unsigned int unsignedIntMaxMinusOne = 4294967294u;
unsigned int unsignedIntMaxHalfRoundedDown = 2147483647u;
unsigned int unsignedIntMaxHalfRoundedUp = 2147483648u;
unsigned int unsignedIntThird = 1431655765u;

// Long (64 bit)
long signedLongMax = 9223372036854775807l;
long signedLongMaxMinusOne = 9223372036854775806l;
long signedLongMin = -9223372036854775806l;
long signedLongMinPlusOne = -9223372036854775805l;
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
long long signedLongLongMin = -9223372036854775806ll;
long long signedLongLongMinPlusOne = -9223372036854775805ll;
long long signedLongLongMaxHalfRoundedUp = 4611686018427387904ll;
long long signedLongLongMaxHalfRoundedDown = 4611686018427387903ll;
long long signedLongLongMaxSeventh = 1317624576693539401l;

unsigned long long unsignedLongLongMax = 18446744073709551615ull;
unsigned long long unsignedLongLongMaxMinusOne = 18446744073709551614ull;
unsigned long long unsignedLongLongMaxHalfRoundedDown = 9223372036854775807ull;
unsigned long long unsignedLongLongMaxHalfRoundedUp = 9223372036854775808ull;
unsigned long long unsignedLongLongThird = 6148914691236517205ull;

// Tests GCC (GNU) builtin overflow functions
int main() {

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
    if ((sIntResult+2) != signedIntMin) {
      printf("Result: %d\n", sIntResult);
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(1, signedIntMax, &sIntResult)) {
      printf("Result: %d\n", sIntResult);
      assert(0);
      goto ERROR;
    }
    if (sIntResult+2 != signedIntMin) {
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
    if (sLongResult + 2l != signedLongMin) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(1l, signedLongMax, &sLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongResult + 2l != signedLongMin) {
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
    if (sLongLongResult + 2ll != signedLongLongMin) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(1ll, signedLongLongMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult + 2ll != signedLongLongMin) {
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
    if (sIntResult != 1) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(signedIntMin, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != 1) {
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
    if (sLongResult != 1l) {
     assert(0);
     goto ERROR;
    }
    if (__builtin_add_overflow(signedLongMax, signedLongMin, &sLongResult)) {
     assert(0);
     goto ERROR;
    }
    if (sLongResult != 1l) {
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
    if (sLongLongResult != 1ll) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(signedLongLongMax, signedLongLongMin, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != 1ll) {
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
    if (sIntResult != 2) {
      assert(0);
      goto ERROR;
    }
    if (__builtin_add_overflow(signedIntMinPlusOne, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != 2) {
      assert(0);
      goto ERROR;
    }
    // (min+1)+max == 2 (all signed long) + reversed
    // (min+1)+max == 2 (all signed long long) + reversed

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
    // (max-1)+1 == max (all signed long) + reversed
    // (max-1)+1 == max (all unsigned long) + reversed
    // (max-1)+1 == max (all signed long long) + reversed
    // (max-1)+1 == max (all unsigned long long) + reversed

    // (max-1)+2 == max+1 overflow (all signed int) + reversed
    if (!__builtin_add_overflow(signedIntMaxMinusOne, 2, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult+2 != signedIntMin) {
      assert(0);
      goto ERROR;
    }

    if (!__builtin_add_overflow(2, signedIntMaxMinusOne, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult+2 != signedIntMin) {
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

    // (max-1)+2 == max+1 overflow (all unsigned int) + reversed
    // (max-1)+2 == max+1 overflow (all signed long) + reversed
    // (smax-1)+2 == smax+1 (input signed long, output unsigned long) + reversed
    // (max-1)+2 == max+1 overflow (all unsigned long) + reversed
    // (max-1)+2 == max+1 overflow (all signed long long) + reversed
    // (smax-1)+2 == smax+1 (input signed long long, output unsigned long long) + reversed
    // (max-1)+2 == max+1 overflow (all unsigned long long) + reversed

    // (max-1)+max == max overflow (all signed int) + reversed
    if (!__builtin_add_overflow(signedIntMaxMinusOne, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedIntMax, signedIntMaxMinusOne, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      assert(0);
      goto ERROR;
    }
    // (max-1)+max == max overflow (all unsigned int) + reversed
    // (max-1)+max == max overflow (all signed long) + reversed
    // (max-1)+max == max overflow (all unsigned long) + reversed
    // (max-1)+max == max overflow (all signed long long) + reversed
    // (max-1)+max == max overflow (all unsigned long long) + reversed

    // max+max == max overflow (all signed int)
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
      assert(0);
      goto ERROR;
    }
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != signedIntMax) {
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
    // max+0 == max overflow (input signed long, output signed int)
    // max+0 == max overflow (input unsigned long, output signed int)
    // max+0 == max overflow (input signed long long, output signed int)
    // max+0 == max overflow (input unsigned long long, output signed int)

    // max+max == max overflow (input signed int, output signed int)
    if (!__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != zero) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input unsigned int, output signed int)
    // max+max == max overflow (input signed long, output signed int)
    // max+max == max overflow (input unsigned long, output signed int)
    // max+max == max overflow (input signed long long, output signed int)
    // max+max == max overflow (input unsigned long long, output signed int)

    // max+max == 2*max signed int (input signed int, output signed long)
    if (__builtin_add_overflow(signedIntMax, signedIntMax, &sIntResult)) {
      assert(0);
      goto ERROR;
    }
    if (sIntResult != zero) {
      assert(0);
      goto ERROR;
    }
    // max+max == 2*max unsigned int (input unsigned int, output signed long)
    // max+max == max overflow (input signed long, output signed long)
    // max+max == max overflow (input unsigned long, output signed long)
    // max+max == max overflow (input signed long long, output signed long)
    // max+max == max overflow (input unsigned long long, output signed long)

    // max+max == 2*max signed int (input signed int, output signed long long)
    if (__builtin_add_overflow(signedIntMax, signedIntMax, &sLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (sLongLongResult != (signedIntMax + signedIntMax)) {
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
    if (uLongLongResult != signedLongLongMax) {
      assert(0);
      goto ERROR;
    }
    // max+max == max overflow (input unsigned long, output unsigned long long)
    if (!__builtin_add_overflow(unsignedLongMax, unsignedLongMax, &uLongLongResult)) {
      assert(0);
      goto ERROR;
    }
    if (uLongLongResult != unsignedLongLongMax) {
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
    unsigned int a;
    if (!__builtin_umul_overflow(4294967295U, 2, &a)) {
      goto ERROR;
    }
    if (a != 4294967295U) {
      goto ERROR;
    }

    if (!__builtin_umul_overflow(2, 4294967295U, &a)) {
      goto ERROR;
    }
    if (a != 4294967295U) {
      goto ERROR;
    }

    if (!__builtin_umul_overflow(1431655765U, 4, &a)) {
      goto ERROR;
    }
    if (a != 4294967295U) {
      goto ERROR;
    }

    if (!__builtin_umul_overflow(4, 1431655765U, &a)) {
      goto ERROR;
    }
    if (a != 4294967295U) {
      goto ERROR;
    }
    
    if (__builtin_umul_overflow(1431655765U, 3, &a)) {
      goto ERROR;
    }
    if (a != 4294967295U) {
      goto ERROR;
    }

    if (__builtin_umul_overflow(3, 1431655765U, &a)) {
      goto ERROR;
    }
    if (a != 4294967295U) {
      goto ERROR;
    }


    if (__builtin_umul_overflow(4294967295U, 1, &a)) {
      goto ERROR;
    }
    if (a != 4294967295U) {
      goto ERROR;
    }

    if (__builtin_umul_overflow(1, 4294967295U, &a)) {
      goto ERROR;
    }
    if (a != 4294967295U) {
      goto ERROR;
    }

  }

  // __builtin_umull_overflow
  {
    unsigned long a;
    if (__builtin_umull_overflow(18446744073709551615ULL, 2, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (!__builtin_umull_overflow(2, 18446744073709551615ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (__builtin_umull_overflow(2, 9223372036854775807ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551614ULL) {
      goto ERROR;
    }


    if (__builtin_umull_overflow(3, 6148914691236517205ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (__builtin_umull_overflow(6148914691236517205ULL, 3, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    // check proper type conversion of arguments
    if (__builtin_umull_overflow(18446744073709551615ULL, 1, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (__builtin_umull_overflow(1, 18446744073709551615ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (!__builtin_umull_overflow(18446744073709551615ULL, 18446744073709551615ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (!__builtin_umull_overflow(18446744073709551615ULL, 18446744073709551615ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

  }

  // __builtin_smulll_overflow
  {
    unsigned long long a;
    if (__builtin_umulll_overflow(18446744073709551615ULL, 2, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (!__builtin_umulll_overflow(2, 18446744073709551615ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (__builtin_umulll_overflow(2, 9223372036854775807ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551614ULL) {
      goto ERROR;
    }


    if (__builtin_umulll_overflow(3, 6148914691236517205ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (__builtin_umulll_overflow(6148914691236517205ULL, 3, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    // check proper type conversion of arguments
    if (__builtin_umulll_overflow(18446744073709551615ULL, 1, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (__builtin_umulll_overflow(1, 18446744073709551615ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (!__builtin_umulll_overflow(18446744073709551615ULL, 18446744073709551615ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }

    if (!__builtin_umulll_overflow(18446744073709551615ULL, 18446744073709551615ULL, &a)) {
      goto ERROR;
    }
    if (a != 18446744073709551615ULL) {
      goto ERROR;
    }
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
