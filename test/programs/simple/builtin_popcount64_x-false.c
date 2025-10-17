// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>


int test_popcount() {
  unsigned int test_int1 = 1231;//10011001111
  unsigned int test_int2 = 0;
  unsigned int test_int3 = 1;

  unsigned int test_uint16BitMaxValue = 65535; // 1111111111111111

  int test_intMax = 2147483647; // 31 times 1
  int test_intMin = -2147483648; // 10000...
  int test_intMinPlusOne = -2147483647; // 10000...001

  unsigned int test_uintMax = 4294967295;  // 32 times 1
  unsigned int test_uintMaxMinusOne = 4294967294;  // 111111....110
  
  int res1 = __builtin_popcount(test_int1) != 7;
  int res2 = __builtin_popcount(test_int2) != 0;
  int res3 = __builtin_popcount(test_int3) != 1;
  int res4 = __builtin_popcount(test_uint16BitMaxValue) != 16;

  int res16 = __builtin_popcount(test_intMax) != 31;
  int res17 = __builtin_popcount(test_intMin) != 1;
  int res18 = __builtin_popcount(test_intMinPlusOne) != 2;
  int res19 = __builtin_popcount(test_uintMax) != 32;
  int res20 = __builtin_popcount(test_uintMaxMinusOne) != 31;

  return res1 || res2 || res3 || res4 || res16 || res17 || res18 || res19 || res20;
}


int test_popcountl() {
  unsigned long test_int1 = 1231;//10011001111
  unsigned long test_int2 = 0;
  unsigned long test_int3 = 1;
  unsigned long test_int4 = 162368;//100111101001000000

  unsigned long test_uint16BitMaxValue = 65535; // 1111111111111111

  int res5 = __builtin_popcountl(test_int1) != 7;
  int res6 = __builtin_popcountl(test_int2) != 0;
  int res7 = __builtin_popcountl(test_int3) != 1;
  int res8 = __builtin_popcountl(test_int4) != 7;
  int res9 = __builtin_popcountl(test_uint16BitMaxValue) != 16;

  long test_longMax = 9223372036854775807L;
  long test_longMin = 0x8000000000000000L; // 1000...0000
  long test_longMinPlusOne = -9223372036854775807L; // 1000...0001

  unsigned long test_ulongMax = 18446744073709551615UL;
  unsigned long test_ulongMaxMinusOne = 18446744073709551614UL;

  int res21 = __builtin_popcountl(test_longMax) != 63;
  int res22 = __builtin_popcountl(test_longMin) != 1;
  int res23 = __builtin_popcountl(test_longMinPlusOne) != 2;
  int res24 = __builtin_popcountl(test_ulongMax) != 64;
  int res25 = __builtin_popcountl(test_ulongMaxMinusOne) != 63;

  return res5 || res6 || res7 || res8 || res9 || res21 || res22 || res23 || res24 || res25;
}


int test_popcountll() {
  unsigned long long test_int1 = 1231;//10011001111
  unsigned long long test_int2 = 0;
  unsigned long long test_int3 = 1;
  unsigned long long test_int4 = 162368;//100111101001000000

  unsigned long long test_uint16BitMaxValue = 65535; // 1111111111111111
  unsigned long long test_uint32BitMaxValue = 4294967295UL; // 11111111111111111111111111111111


  int res10 = __builtin_popcountll(test_int1) != 7;
  int res11 = __builtin_popcountll(test_int2) != 0;
  int res12 = __builtin_popcountll(test_int3) != 1;
  int res13 = __builtin_popcountll(test_int4) != 7;

  int res14 = __builtin_popcountll(test_uint16BitMaxValue) != 16;
  int res15 = __builtin_popcountll(test_uint32BitMaxValue) != 32;

  long long test_longLongMax = 9223372036854775807LL;
  long long test_longLongMin = 0x8000000000000000LL; // 1000...0000
  long long test_longLongMinPlusOne = -9223372036854775807LL; // 1000...0001

  unsigned long long test_ulongLongMax = 18446744073709551615ULL;
  unsigned long long test_ulongLongMaxMinusOne = 18446744073709551614ULL;

  int res26 = __builtin_popcountll(test_longLongMax) != 63;
  int res27 = __builtin_popcountll(test_longLongMin) != 1;
  int res28 = __builtin_popcountll(test_longLongMinPlusOne) != 2;
  int res29 = __builtin_popcountll(test_ulongLongMax) != 64;
  int res30 = __builtin_popcountll(test_ulongLongMaxMinusOne) != 63;

  return res10 || res11 || res12 || res13 || res14 || res15 || res26 || res27 || res28 || res29 || res30;
}

// GCC builtin function -> use GCC to compile!
int main() {
  int resInt = test_popcount();
  int resLong = test_popcountl();
  int resLongLong = test_popcountll();
  assert(resInt || resLong || resLongLong); // Only wrong if ALL are wrong
  return 0;
}