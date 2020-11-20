// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __builtin_popcountll (unsigned long long);
extern int __builtin_popcountl (unsigned long);
extern int __builtin_popcount (unsigned int x);
void reach_error() { __assert_fail("0", "builtin_popcount_x.c", 25, "reach_error"); }
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: {reach_error();abort();} } }

int main() {
  test_popcount();
  return 0;
}

void test_popcount(){
  unsigned int test_int1 = 1231;//10011001111
  unsigned int test_int2 = 0;
  unsigned int test_int3 = 1;
  unsigned int test_int4 = 162368;//100111101001000000
  
  __VERIFIER_assert(____builtin_popcount(test_int1) == 7);
  __VERIFIER_assert(____builtin_popcount(test_int2) == 0);
  __VERIFIER_assert(____builtin_popcount(test_int3) == 1);
  __VERIFIER_assert(____builtin_popcount(test_int4) == 7);
}


void test_popcountl(){
  unsigned long test_int1 = 1231;//10011001111
  unsigned long test_int2 = 0;
  unsigned long test_int3 = 1;
  unsigned long test_int4 = 162368;//100111101001000000
  
  __VERIFIER_assert(____builtin_popcountl(test_int1) == 7);
  __VERIFIER_assert(____builtin_popcountl(test_int2) == 0);
  __VERIFIER_assert(____builtin_popcountl(test_int3) == 1);
  __VERIFIER_assert(____builtin_popcountl(test_int4) == 7);
}


void test_popcountll(){
  unsigned long long test_int1 = 1231;//10011001111
  unsigned long long test_int2 = 0;
  unsigned long long test_int3 = 1;
  unsigned long long test_int4 = 162368;//100111101001000000
  
  __VERIFIER_assert(____builtin_popcountll(test_int1) == 7);
  __VERIFIER_assert(____builtin_popcountll(test_int2) == 0);
  __VERIFIER_assert(____builtin_popcountll(test_int3) == 1);
  __VERIFIER_assert(____builtin_popcountll(test_int4) == 7);
}


