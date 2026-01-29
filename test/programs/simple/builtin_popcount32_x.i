// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert_perror_fail (int __errnum, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void test_popcount() {
  unsigned int test_int1 = 1231;
  unsigned int test_int2 = 0;
  unsigned int test_int3 = 1;
  unsigned int test_uint16BitMaxValue = 65535;
  ((__builtin_popcount(test_int1) == 7) ? (void) (0) : __assert_fail ("__builtin_popcount(test_int1) == 7", "builtin_popcount32_x.c", 19, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcount(test_int2) == 0) ? (void) (0) : __assert_fail ("__builtin_popcount(test_int2) == 0", "builtin_popcount32_x.c", 20, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcount(test_int3) == 1) ? (void) (0) : __assert_fail ("__builtin_popcount(test_int3) == 1", "builtin_popcount32_x.c", 21, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcount(test_uint16BitMaxValue) == 16) ? (void) (0) : __assert_fail ("__builtin_popcount(test_uint16BitMaxValue) == 16", "builtin_popcount32_x.c", 22, __extension__ __PRETTY_FUNCTION__));
  int test_intMax = 2147483647;
  int test_intMin = -2147483648;
  int test_intMinPlusOne = -2147483647;
  unsigned int test_uintMax = 4294967295;
  unsigned int test_uintMaxMinusOne = 4294967294;
  ((__builtin_popcount(test_intMax) == 31) ? (void) (0) : __assert_fail ("__builtin_popcount(test_intMax) == 31", "builtin_popcount32_x.c", 31, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcount(test_intMin) == 1) ? (void) (0) : __assert_fail ("__builtin_popcount(test_intMin) == 1", "builtin_popcount32_x.c", 32, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcount(test_intMinPlusOne) == 2) ? (void) (0) : __assert_fail ("__builtin_popcount(test_intMinPlusOne) == 2", "builtin_popcount32_x.c", 33, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcount(test_uintMax) == 32) ? (void) (0) : __assert_fail ("__builtin_popcount(test_uintMax) == 32", "builtin_popcount32_x.c", 34, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcount(test_uintMaxMinusOne) == 31) ? (void) (0) : __assert_fail ("__builtin_popcount(test_uintMaxMinusOne) == 31", "builtin_popcount32_x.c", 35, __extension__ __PRETTY_FUNCTION__));
}
void test_popcountl() {
  unsigned long test_int1 = 1231;
  unsigned long test_int2 = 0;
  unsigned long test_int3 = 1;
  unsigned long test_int4 = 162368;
  unsigned long test_uint16BitMaxValue = 65535;
  ((__builtin_popcountl(test_int1) == 7) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_int1) == 7", "builtin_popcount32_x.c", 46, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountl(test_int2) == 0) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_int2) == 0", "builtin_popcount32_x.c", 47, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountl(test_int3) == 1) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_int3) == 1", "builtin_popcount32_x.c", 48, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountl(test_int4) == 7) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_int4) == 7", "builtin_popcount32_x.c", 49, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountl(test_uint16BitMaxValue) == 16) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_uint16BitMaxValue) == 16", "builtin_popcount32_x.c", 50, __extension__ __PRETTY_FUNCTION__));
  long test_longMax = 2147483647L;
  long test_longMin = -2147483648L;
  long test_longMinPlusOne = -2147483647L;
  unsigned long test_ulongMax = 4294967295UL;
  unsigned long test_ulongMaxMinusOne = 4294967294UL;
  ((__builtin_popcountl(test_longMax) == 31) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_longMax) == 31", "builtin_popcount32_x.c", 59, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountl(test_longMin) == 1) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_longMin) == 1", "builtin_popcount32_x.c", 60, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountl(test_longMinPlusOne) == 2) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_longMinPlusOne) == 2", "builtin_popcount32_x.c", 61, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountl(test_ulongMax) == 32) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_ulongMax) == 32", "builtin_popcount32_x.c", 62, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountl(test_ulongMaxMinusOne) == 31) ? (void) (0) : __assert_fail ("__builtin_popcountl(test_ulongMaxMinusOne) == 31", "builtin_popcount32_x.c", 63, __extension__ __PRETTY_FUNCTION__));
}
void test_popcountll() {
  unsigned long long test_int1 = 1231;
  unsigned long long test_int2 = 0;
  unsigned long long test_int3 = 1;
  unsigned long long test_int4 = 162368;
  unsigned long long test_uint16BitMaxValue = 65535;
  unsigned long long test_uint32BitMaxValue = 4294967295UL;
  ((__builtin_popcountll(test_int1) == 7) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_int1) == 7", "builtin_popcount32_x.c", 76, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_int2) == 0) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_int2) == 0", "builtin_popcount32_x.c", 77, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_int3) == 1) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_int3) == 1", "builtin_popcount32_x.c", 78, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_int4) == 7) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_int4) == 7", "builtin_popcount32_x.c", 79, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_uint16BitMaxValue) == 16) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_uint16BitMaxValue) == 16", "builtin_popcount32_x.c", 81, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_uint32BitMaxValue) == 32) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_uint32BitMaxValue) == 32", "builtin_popcount32_x.c", 82, __extension__ __PRETTY_FUNCTION__));
  long long test_longLongMax = 9223372036854775807LL;
  long long test_longLongMin = 0x8000000000000000LL;
  long long test_longLongMinPlusOne = -9223372036854775807LL;
  long long test_longLongMinusOne = -1;
  unsigned long long test_ulongLongMax = 18446744073709551615ULL;
  unsigned long long test_ulongLongMaxMinusOne = 18446744073709551614ULL;
  ((__builtin_popcountll(test_longLongMax) == 63) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_longLongMax) == 63", "builtin_popcount32_x.c", 92, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_longLongMinPlusOne) == 2) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_longLongMinPlusOne) == 2", "builtin_popcount32_x.c", 93, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_longLongMinusOne) == 64) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_longLongMinusOne) == 64", "builtin_popcount32_x.c", 94, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_ulongLongMax) == 64) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_ulongLongMax) == 64", "builtin_popcount32_x.c", 95, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_ulongLongMaxMinusOne) == 63) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_ulongLongMaxMinusOne) == 63", "builtin_popcount32_x.c", 96, __extension__ __PRETTY_FUNCTION__));
  ((__builtin_popcountll(test_longLongMin) == 1) ? (void) (0) : __assert_fail ("__builtin_popcountll(test_longLongMin) == 1", "builtin_popcount32_x.c", 97, __extension__ __PRETTY_FUNCTION__));
}
int main() {
  test_popcount();
  test_popcountl();
  test_popcountll();
  return 0;
}
