// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>

void reach_error() { assert(0); }

void __VERIFIER_assert(int cond) {
    if (!(cond)) {
          ERROR: {reach_error();abort();}
                   }
      return;
}

void test_popcount() {
  unsigned int test_int1 = 1231;//10011001111
  unsigned int test_int2 = 0;
  unsigned int test_int3 = 1;

  unsigned int test_uint16BitMaxValue = 65535; // 1111111111111111
  
  __VERIFIER_assert(__builtin_popcount(test_int1) == 7);
  __VERIFIER_assert(__builtin_popcount(test_int2) == 0);
  __VERIFIER_assert(__builtin_popcount(test_int3) == 1);
  __VERIFIER_assert(__builtin_popcount(test_uint16BitMaxValue) == 16);

  int test_intMax = 2147483647; // 31 times 1
  int test_intMin = -2147483648; // 10000...
  int test_intMinPlusOne = -2147483647; // 10000...001

  unsigned int test_uintMax = 4294967295;  // 32 times 1
  unsigned int test_uintMaxMinusOne = 4294967294;  // 111111....110

  __VERIFIER_assert(__builtin_popcount(test_intMax) == 31);
  __VERIFIER_assert(__builtin_popcount(test_intMin) == 1);
  __VERIFIER_assert(__builtin_popcount(test_intMinPlusOne) == 2);
  __VERIFIER_assert(__builtin_popcount(test_uintMax) == 32);
  __VERIFIER_assert(__builtin_popcount(test_uintMaxMinusOne) == 31);
}

void test_popcountl() {
  unsigned long test_int1 = 1231;//10011001111
  unsigned long test_int2 = 0;
  unsigned long test_int3 = 1;
  unsigned long test_int4 = 162368;//100111101001000000

  unsigned long test_uint16BitMaxValue = 65535; // 1111111111111111

  __VERIFIER_assert(__builtin_popcountl(test_int1) == 7);
  __VERIFIER_assert(__builtin_popcountl(test_int2) == 0);
  __VERIFIER_assert(__builtin_popcountl(test_int3) == 1);
  __VERIFIER_assert(__builtin_popcountl(test_int4) == 7);
  __VERIFIER_assert(__builtin_popcountl(test_uint16BitMaxValue) == 16);

  long test_longMax = 2147483647L;
  long test_longMin = -2147483648L;
  long test_longMinPlusOne = -2147483647L;

  unsigned long test_ulongMax = 4294967295UL;
  unsigned long test_ulongMaxMinusOne = 4294967294UL;

  __VERIFIER_assert(__builtin_popcountl(test_longMax) == 31);
  __VERIFIER_assert(__builtin_popcountl(test_longMin) == 1);
  __VERIFIER_assert(__builtin_popcountl(test_longMinPlusOne) == 2);
  __VERIFIER_assert(__builtin_popcountl(test_ulongMax) == 32);
  __VERIFIER_assert(__builtin_popcountl(test_ulongMaxMinusOne) == 31);
}

void test_popcountll() {
  unsigned long long test_int1 = 1231;//10011001111
  unsigned long long test_int2 = 0;
  unsigned long long test_int3 = 1;
  unsigned long long test_int4 = 162368;//100111101001000000

  unsigned long long test_uint16BitMaxValue = 65535; // 1111111111111111
  unsigned long long test_uint32BitMaxValue = 4294967295UL; // 11111111111111111111111111111111


  __VERIFIER_assert(__builtin_popcountll(test_int1) == 7);
  __VERIFIER_assert(__builtin_popcountll(test_int2) == 0);
  __VERIFIER_assert(__builtin_popcountll(test_int3) == 1);
  __VERIFIER_assert(__builtin_popcountll(test_int4) == 7);

  __VERIFIER_assert(__builtin_popcountll(test_uint16BitMaxValue) == 16);
  __VERIFIER_assert(__builtin_popcountll(test_uint32BitMaxValue) == 32);

  long long test_longLongMax = 9223372036854775807LL;
  long long test_longLongMin = 0x8000000000000000LL; // 1000...0000
  long long test_longLongMinPlusOne = -9223372036854775807LL; // 1000...0001
  long long test_longLongMinusOne = -1;  // 1111...1111

  unsigned long long test_ulongLongMax = 18446744073709551615ULL;
  unsigned long long test_ulongLongMaxMinusOne = 18446744073709551614ULL;

  __VERIFIER_assert(__builtin_popcountll(test_longLongMax) == 63);
  __VERIFIER_assert(__builtin_popcountll(test_longLongMinPlusOne) == 2);
  __VERIFIER_assert(__builtin_popcountll(test_longLongMinusOne) == 64);
  __VERIFIER_assert(__builtin_popcountll(test_ulongLongMax) == 64);
  __VERIFIER_assert(__builtin_popcountll(test_ulongLongMaxMinusOne) == 63);
  __VERIFIER_assert(__builtin_popcountll(test_longLongMin) == 1);
}

// GCC builtin function -> use GCC to compile!
int main() {
  test_popcount();
  // test_popcountl();
  // test_popcountll();
  return 0;
}