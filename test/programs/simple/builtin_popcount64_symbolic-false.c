// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>

extern unsigned int __VERIFIER_nondet_uint();
extern int __VERIFIER_nondet_int();

extern unsigned long __VERIFIER_nondet_ulong();
extern long __VERIFIER_nondet_long();

extern unsigned long long __VERIFIER_nondet_ulonglong();
extern long long __VERIFIER_nondet_longlong();


int test_popcount() {
  unsigned int test_int1 = 1231;//10011001111
  unsigned int test_int2 = 0;
  unsigned int test_int3 = 1;

  unsigned int test_uint16BitMaxValue = 65535; // 1111111111111111
  
  int nondet_int = __VERIFIER_nondet_int();
  if (nondet_int != test_int1 && nondet_int != test_int2 && nondet_int != test_int3 && nondet_int != test_uint16BitMaxValue) {
    exit(0);
  }

  int intRes1 = __builtin_popcount(nondet_int) == 7;
  int intRes2 = __builtin_popcount(nondet_int) == 16;
  int intRes3 = __builtin_popcount(nondet_int) == 0;
  int intRes4 = __builtin_popcount(nondet_int) == 1;
  int res1 = intRes1 && intRes2 && intRes3 && intRes4;


  unsigned int nondet_uint = __VERIFIER_nondet_uint();
  if (nondet_uint != test_int1 && nondet_uint != test_int2 && nondet_uint != test_int3 && nondet_uint != test_uint16BitMaxValue) {
    exit(0);
  }

  int uintRes1 = __builtin_popcount(nondet_uint) != 7;
  int uintRes2 = __builtin_popcount(nondet_uint) != 16;
  int uintRes3 = __builtin_popcount(nondet_uint) != 0;
  int uintRes4 = __builtin_popcount(nondet_uint) != 1;
  int res2 = uintRes1 && uintRes2 && uintRes3 && uintRes4;

  int nondet_intMax = __VERIFIER_nondet_int();
  if (!(nondet_intMax > 2147483646)) {
    exit(0);
  }
  int res3 = __builtin_popcount(nondet_intMax) != 31;

  int nondet_intMin = __VERIFIER_nondet_int();
  if (!(nondet_intMin < -2147483647)) {
    exit(0);
  }
  int res4 = __builtin_popcount(nondet_intMin) != 1;

  int nondet_intAtLeastMinPlusOne = __VERIFIER_nondet_int();
  if (!(nondet_intAtLeastMinPlusOne < -2147483646)) {
    exit(0);
  }
  int count_intAtLeastMinPlusOne = __builtin_popcount(nondet_intAtLeastMinPlusOne);
  if (count_intAtLeastMinPlusOne == 1) {
    exit(0);
  }
  int res5 = count_intAtLeastMinPlusOne != 2;

  unsigned int nondet_uintMax = __VERIFIER_nondet_uint();
  if (!(nondet_uintMax > 4294967294)) {
    exit(0);
  }
  int res6 = __builtin_popcount(nondet_uintMax) != 32;

  unsigned int nondet_uintAtLeastMaxMinusOne = __VERIFIER_nondet_uint();
  if (!(nondet_uintAtLeastMaxMinusOne > 4294967293)) {
    exit(0);
  }
  int count_uintAtLeastMaxMinusOne = __builtin_popcount(nondet_uintAtLeastMaxMinusOne);
  int res7 = count_uintAtLeastMaxMinusOne != 31 && count_uintAtLeastMaxMinusOne != 32;

  return res1 || res2 || res3 || res4 || res5 || res6 || res7;
}

int test_popcountl() {
  unsigned long test_int1 = 1231;//10011001111
  unsigned long test_int2 = 0;
  unsigned long test_int3 = 1;
  unsigned long test_int4 = 162368;//100111101001000000

  unsigned long test_uint16BitMaxValue = 65535; // 1111111111111111
  unsigned long test_uint32BitMaxValue = 4294967295UL; // 11111111111111111111111111111111


  long nondet_long = __VERIFIER_nondet_long();
  if (nondet_long != test_int1 && nondet_long != test_int2 && nondet_long != test_int3 && nondet_long != test_int4 && nondet_long != test_uint16BitMaxValue && nondet_long != test_uint32BitMaxValue) {
    exit(0);
  }

  int longRes1 = __builtin_popcountl(nondet_long) != 7;
  int longRes2 = __builtin_popcountl(nondet_long) != 0;
  int longRes3 = __builtin_popcountl(nondet_long) != 1;
  int longRes4 = __builtin_popcountl(nondet_long) != 7;
  int longRes5 = __builtin_popcountl(nondet_long) != 16;
  int longRes6 = __builtin_popcountl(nondet_long) != 32;
  int res1 = longRes1 && longRes2 && longRes3 && longRes4 && longRes5 && longRes6;


  unsigned long nondet_ul = __VERIFIER_nondet_ulong();
  if (nondet_ul != test_int1 && nondet_ul != test_int2 && nondet_ul != test_int3 && nondet_ul != test_int4 && nondet_ul != test_uint16BitMaxValue) {
    exit(0);
  }

  int ulongRes1 = __builtin_popcountl(nondet_ul) != 7;
  int ulongRes2 = __builtin_popcountl(nondet_ul) != 0;
  int ulongRes3 = __builtin_popcountl(nondet_ul) != 1;
  int ulongRes4 = __builtin_popcountl(nondet_ul) != 7;
  int ulongRes5 = __builtin_popcountl(nondet_ul) != 16;
  int res2 = ulongRes1 && ulongRes2 && ulongRes3 && ulongRes4 && ulongRes5;

  long nondet_longMax = __VERIFIER_nondet_long();
  if (!(nondet_longMax > 9223372036854775806L)) {
    exit(0);
  }
  int res3 = __builtin_popcountl(nondet_longMax) != 63;

  long nondet_lMin = __VERIFIER_nondet_long();
  if (!(nondet_lMin < -9223372036854775807L)) {
    exit(0);
  }
  int res4 = __builtin_popcountl(nondet_lMin) != 1;

  long nondet_lAtLeastMinPlusOne = __VERIFIER_nondet_long();
  if (!(nondet_lAtLeastMinPlusOne < -9223372036854775806L)) {
    exit(0);
  }
  int count_lAtLeastMinPlusOne = __builtin_popcountl(nondet_lAtLeastMinPlusOne);
  if (count_lAtLeastMinPlusOne == 1) {
    exit(0);
  }
  int res5 = count_lAtLeastMinPlusOne != 2;

  long nondet_lMin2 = __VERIFIER_nondet_long();
  if (!(nondet_lMin2 < 0 && nondet_lMin2 >= -2L)) { // -1 is 1111....1111, -2 is 1111...1110
    exit(0);
  }
  int res6 = __builtin_popcountl(nondet_lMin2) != 64 && __builtin_popcountl(nondet_lMin2) != 63;

  unsigned long nondet_ulMax = __VERIFIER_nondet_ulong();
  if (!(nondet_ulMax > 18446744073709551614UL)) {
    exit(0);
  }
  int res7 = __builtin_popcountl(nondet_ulMax) != 64;

  unsigned long nondet_ulAtLeastMaxMinusOne = __VERIFIER_nondet_ulong();
  if (!(nondet_ulAtLeastMaxMinusOne > 18446744073709551613UL)) {
    exit(0);
  }
  int count_ulAtLeastMaxMinusOne = __builtin_popcountl(nondet_ulAtLeastMaxMinusOne);
  int res8 = count_ulAtLeastMaxMinusOne != 63 && count_ulAtLeastMaxMinusOne != 64;

  return res1 || res2 || res3 || res4 || res5 || res6 || res7 || res8;
}

int test_popcountll() {
  unsigned long long test_int1 = 1231;//10011001111
  unsigned long long test_int2 = 0;
  unsigned long long test_int3 = 1;
  unsigned long long test_int4 = 162368;//100111101001000000

  unsigned long long test_uint16BitMaxValue = 65535; // 1111111111111111
  unsigned long long test_uint32BitMaxValue = 4294967295UL; // 11111111111111111111111111111111


  long long nondet_ll = __VERIFIER_nondet_longlong();
  if (nondet_ll != test_int1 && nondet_ll != test_int2 && nondet_ll != test_int3 && nondet_ll != test_int4 && nondet_ll != test_uint16BitMaxValue && nondet_ll != test_uint32BitMaxValue) {
    exit(0);
  }

  int longRes1 = __builtin_popcountll(nondet_ll) != 7;
  int longRes2 = __builtin_popcountll(nondet_ll) != 0;
  int longRes3 = __builtin_popcountll(nondet_ll) != 1;
  int longRes4 = __builtin_popcountll(nondet_ll) != 7;
  int longRes5 = __builtin_popcountll(nondet_ll) != 16;
  int longRes6 = __builtin_popcountll(nondet_ll) != 32;
  int res1 = longRes1 && longRes2 && longRes3 && longRes4 && longRes5 && longRes6;


  unsigned long long nondet_ull = __VERIFIER_nondet_ulonglong();
  if (nondet_ull != test_int1 && nondet_ull != test_int2 && nondet_ull != test_int3 && nondet_ull != test_int4 && nondet_ull != test_uint16BitMaxValue) {
    exit(0);
  }

  int ullRes1 = __builtin_popcountll(nondet_ull) != 7;
  int ullRes2 = __builtin_popcountll(nondet_ull) != 0;
  int ullRes3 = __builtin_popcountll(nondet_ull) != 1;
  int ullRes4 = __builtin_popcountll(nondet_ull) != 7;
  int ullRes5 = __builtin_popcountll(nondet_ull) != 16;
  int res2 = ullRes1 && ullRes2 && ullRes3 && ullRes4 && ullRes5;

  long long nondet_llMax = __VERIFIER_nondet_longlong();
  if (!(nondet_llMax > 9223372036854775806LL)) {
    exit(0);
  }
  int res3 = __builtin_popcountll(nondet_llMax) != 63;

  long long nondet_llMin = __VERIFIER_nondet_longlong();
  if (!(nondet_llMin < -9223372036854775807LL)) {
    exit(0);
  }
  int res4 = __builtin_popcountll(nondet_llMin) != 1;

  long long nondet_llAtLeastMinPlusOne = __VERIFIER_nondet_longlong();
  if (!(nondet_llAtLeastMinPlusOne < -9223372036854775806LL)) {
    exit(0);
  }
  int count_llAtLeastMinPlusOne = __builtin_popcountll(nondet_llAtLeastMinPlusOne);
  if (count_llAtLeastMinPlusOne == 1) {
    exit(0);
  }
  int res5 = count_llAtLeastMinPlusOne != 2;

  long long nondet_llMin2 = __VERIFIER_nondet_ulonglong();
  if (!(nondet_llMin2 < 0 && nondet_llMin2 >= -2LL)) { // -1 is 1111....1111, -2 is 1111...1110
    exit(0);
  }
  int res6 = __builtin_popcountll(nondet_llMin2) != 64 && __builtin_popcountll(nondet_llMin2) != 63;

  unsigned long long nondet_ullMax = __VERIFIER_nondet_ulonglong();
  if (!(nondet_ullMax > 18446744073709551614ULL)) {
    exit(0);
  }
  int res7 = __builtin_popcountll(nondet_ullMax) != 64;

  unsigned long long nondet_ullAtLeastMaxMinusOne = __VERIFIER_nondet_ulonglong();
  if (!(nondet_ullAtLeastMaxMinusOne > 18446744073709551613ULL)) {
    exit(0);
  }
  int count_ullAtLeastMaxMinusOne = __builtin_popcountll(nondet_ullAtLeastMaxMinusOne);
  int res8 = count_ullAtLeastMaxMinusOne != 63 && count_ullAtLeastMaxMinusOne != 64;

  return res1 || res2 || res3 || res4 || res5 || res6 || res7 || res8;
}

// GCC builtin function -> use GCC to compile!
int main() {
  int resInt = test_popcount();
  int resLong = test_popcountl();
  int resLongLong = test_popcountll();
  
  // Violates if there is no true result in any of the 3 functions
  assert(resInt || resLong || resLongLong);
  return 0;
}
