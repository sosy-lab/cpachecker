// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
#include <assert.h>

extern int __builtin_popcountll (unsigned long long);
extern int __builtin_popcountl (unsigned long);
extern int __builtin_popcount (unsigned int x);


int main() {
  test_popcount();
  test_popcountl();
  test_popcountll();
  return 0;
}

void test_popcount(){
  unsigned int test_int1 = 1231;//10011001111
  unsigned int test_int2 = 0;
  unsigned int test_int3 = 1;

  unsigned int test_uint16BitMaxValue = 65535; // 1111111111111111
  
  assert(__builtin_popcount(test_int1) == 7);
  assert(__builtin_popcount(test_int2) == 0);
  assert(__builtin_popcount(test_int3) == 1);
  assert(__builtin_popcount(test_uint16BitMaxValue) == 16);
}


void test_popcountl(){
  unsigned long test_int1 = 1231;//10011001111
  unsigned long test_int2 = 0;
  unsigned long test_int3 = 1;
  unsigned long test_int4 = 162368;//100111101001000000

  unsigned long test_uint16BitMaxValue = 65535; // 1111111111111111
  unsigned long test_uint32BitMaxValue = 4294967295UL; // 11111111111111111111111111111111

  assert(__builtin_popcountl(test_int1) == 7);
  assert(__builtin_popcountl(test_int2) == 0);
  assert(__builtin_popcountl(test_int3) == 1);
  assert(__builtin_popcountl(test_int4) == 7);
  assert(__builtin_popcountl(test_uint16BitMaxValue) == 16);
  assert(__builtin_popcountl(test_uint32BitMaxValue) == 32);

}


void test_popcountll(){
  unsigned long long test_int1 = 1231;//10011001111
  unsigned long long test_int2 = 0;
  unsigned long long test_int3 = 1;
  unsigned long long test_int4 = 162368;//100111101001000000

  unsigned long long test_uint16BitMaxValue = 65535; // 1111111111111111
  unsigned long long test_uint32BitMaxValue = 4294967295UL; // 11111111111111111111111111111111
  unsigned long long test_uint64BitMaxValue = 18446744073709551615ULL; // 1111111111111111111111111111111111111111111111111111111111111111


  assert(__builtin_popcountll(test_int1) == 7);
  assert(__builtin_popcountll(test_int2) == 0);
  assert(__builtin_popcountll(test_int3) == 1);
  assert(__builtin_popcountll(test_int4) == 7);

  assert(__builtin_popcountll(test_uint16BitMaxValue) == 16);
  assert(__builtin_popcountll(test_uint32BitMaxValue) == 32);
  assert(__builtin_popcountll(test_uint64BitMaxValue) == 64);
}


