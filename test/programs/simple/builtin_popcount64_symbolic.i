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

extern unsigned int __VERIFIER_nondet_uint();
extern int __VERIFIER_nondet_int();
extern unsigned long __VERIFIER_nondet_ulong();
extern long __VERIFIER_nondet_long();
extern unsigned long long __VERIFIER_nondet_ulonglong();
extern long long __VERIFIER_nondet_longlong();
void test_popcount() {
  unsigned int test_int1 = 1231;
  unsigned int test_int2 = 0;
  unsigned int test_int3 = 1;
  unsigned int test_uint16BitMaxValue = 65535;
  int nondet_int = __VERIFIER_nondet_int();
  if (nondet_int != test_int1 && nondet_int != test_int2 && nondet_int != test_int3 && nondet_int != test_uint16BitMaxValue) {
    return;
  }
  int intRes1 = __builtin_popcount(nondet_int) == 7;
  int intRes2 = __builtin_popcount(nondet_int) == 16;
  int intRes3 = __builtin_popcount(nondet_int) == 0;
  int intRes4 = __builtin_popcount(nondet_int) == 1;
  ((intRes1 || intRes2 || intRes3 || intRes4) ? (void) (0) : __assert_fail ("intRes1 || intRes2 || intRes3 || intRes4", "builtin_popcount64_symbolic.c", 37, __extension__ __PRETTY_FUNCTION__));
  unsigned int nondet_uint = __VERIFIER_nondet_uint();
  if (nondet_uint != test_int1 && nondet_uint != test_int2 && nondet_uint != test_int3 && nondet_uint != test_uint16BitMaxValue) {
    return;
  }
  int uintRes1 = __builtin_popcount(nondet_uint) == 7;
  int uintRes2 = __builtin_popcount(nondet_uint) == 16;
  int uintRes3 = __builtin_popcount(nondet_uint) == 0;
  int uintRes4 = __builtin_popcount(nondet_uint) == 1;
  ((uintRes1 || uintRes2 || uintRes3 || uintRes4) ? (void) (0) : __assert_fail ("uintRes1 || uintRes2 || uintRes3 || uintRes4", "builtin_popcount64_symbolic.c", 49, __extension__ __PRETTY_FUNCTION__));
  int nondet_intMax = __VERIFIER_nondet_int();
  if (!(nondet_intMax > 2147483646)) {
    return;
  }
  ((__builtin_popcount(nondet_intMax) == 31) ? (void) (0) : __assert_fail ("__builtin_popcount(nondet_intMax) == 31", "builtin_popcount64_symbolic.c", 55, __extension__ __PRETTY_FUNCTION__));
  int nondet_intMin = __VERIFIER_nondet_int();
  if (!(nondet_intMin < -2147483647)) {
    return;
  }
  ((__builtin_popcount(nondet_intMin) == 1) ? (void) (0) : __assert_fail ("__builtin_popcount(nondet_intMin) == 1", "builtin_popcount64_symbolic.c", 61, __extension__ __PRETTY_FUNCTION__));
  int nondet_intAtLeastMinPlusOne = __VERIFIER_nondet_int();
  if (!(nondet_intAtLeastMinPlusOne < -2147483646)) {
    return;
  }
  int count_intAtLeastMinPlusOne = __builtin_popcount(nondet_intAtLeastMinPlusOne);
  if (count_intAtLeastMinPlusOne == 1) {
    return;
  }
  ((count_intAtLeastMinPlusOne == 2) ? (void) (0) : __assert_fail ("count_intAtLeastMinPlusOne == 2", "builtin_popcount64_symbolic.c", 71, __extension__ __PRETTY_FUNCTION__));
  int nondet_uintMax = __VERIFIER_nondet_uint();
  if (!(nondet_uintMax > 4294967294)) {
    return;
  }
  ((__builtin_popcount(nondet_uintMax) == 32) ? (void) (0) : __assert_fail ("__builtin_popcount(nondet_uintMax) == 32", "builtin_popcount64_symbolic.c", 77, __extension__ __PRETTY_FUNCTION__));
  int nondet_uintAtLeastMaxMinusOne = __VERIFIER_nondet_uint();
  if (!(nondet_uintAtLeastMaxMinusOne > 4294967293)) {
    return;
  }
  int count_uintAtLeastMaxMinusOne = __builtin_popcount(nondet_uintAtLeastMaxMinusOne);
  ((count_uintAtLeastMaxMinusOne == 31 || count_uintAtLeastMaxMinusOne == 32) ? (void) (0) : __assert_fail ("count_uintAtLeastMaxMinusOne == 31 || count_uintAtLeastMaxMinusOne == 32", "builtin_popcount64_symbolic.c", 84, __extension__ __PRETTY_FUNCTION__));
}
void test_popcountl() {
  unsigned long test_int1 = 1231;
  unsigned long test_int2 = 0;
  unsigned long test_int3 = 1;
  unsigned long test_int4 = 162368;
  unsigned long test_uint16BitMaxValue = 65535;
  unsigned long test_uint32BitMaxValue = 4294967295UL;
  long nondet_long = __VERIFIER_nondet_long();
  if (nondet_long != test_int1 && nondet_long != test_int2 && nondet_long != test_int3 && nondet_long != test_int4 && nondet_long != test_uint16BitMaxValue && nondet_long != test_uint32BitMaxValue) {
    return;
  }
  int longRes1 = __builtin_popcountl(nondet_long) == 7;
  int longRes2 = __builtin_popcountl(nondet_long) == 0;
  int longRes3 = __builtin_popcountl(nondet_long) == 1;
  int longRes4 = __builtin_popcountl(nondet_long) == 7;
  int longRes5 = __builtin_popcountl(nondet_long) == 16;
  int longRes6 = __builtin_popcountl(nondet_long) == 32;
  ((longRes1 || longRes2 || longRes3 || longRes4 || longRes5 || longRes6) ? (void) (0) : __assert_fail ("longRes1 || longRes2 || longRes3 || longRes4 || longRes5 || longRes6", "builtin_popcount64_symbolic.c", 108, __extension__ __PRETTY_FUNCTION__));
  unsigned long nondet_ul = __VERIFIER_nondet_ulong();
  if (nondet_ul != test_int1 && nondet_ul != test_int2 && nondet_ul != test_int3 && nondet_ul != test_int4 && nondet_ul != test_uint16BitMaxValue) {
    return;
  }
  int ulongRes1 = __builtin_popcountl(nondet_ul) == 7;
  int ulongRes2 = __builtin_popcountl(nondet_ul) == 0;
  int ulongRes3 = __builtin_popcountl(nondet_ul) == 1;
  int ulongRes4 = __builtin_popcountl(nondet_ul) == 7;
  int ulongRes5 = __builtin_popcountl(nondet_ul) == 16;
  ((ulongRes1 || ulongRes2 || ulongRes3 || ulongRes4 || ulongRes5) ? (void) (0) : __assert_fail ("ulongRes1 || ulongRes2 || ulongRes3 || ulongRes4 || ulongRes5", "builtin_popcount64_symbolic.c", 121, __extension__ __PRETTY_FUNCTION__));
  long nondet_longMax = __VERIFIER_nondet_long();
  if (!(nondet_longMax > 9223372036854775806L)) {
    return;
  }
  ((__builtin_popcountl(nondet_longMax) == 63) ? (void) (0) : __assert_fail ("__builtin_popcountl(nondet_longMax) == 63", "builtin_popcount64_symbolic.c", 127, __extension__ __PRETTY_FUNCTION__));
  long nondet_lMin = __VERIFIER_nondet_long();
  if (!(nondet_lMin < -9223372036854775807L)) {
    return;
  }
  ((__builtin_popcountl(nondet_lMin) == 1) ? (void) (0) : __assert_fail ("__builtin_popcountl(nondet_lMin) == 1", "builtin_popcount64_symbolic.c", 133, __extension__ __PRETTY_FUNCTION__));
  long nondet_lAtLeastMinPlusOne = __VERIFIER_nondet_long();
  if (!(nondet_lAtLeastMinPlusOne < -9223372036854775806L)) {
    return;
  }
  int count_lAtLeastMinPlusOne = __builtin_popcountl(nondet_lAtLeastMinPlusOne);
  if (count_lAtLeastMinPlusOne == 1) {
    return;
  }
  ((count_lAtLeastMinPlusOne == 2) ? (void) (0) : __assert_fail ("count_lAtLeastMinPlusOne == 2", "builtin_popcount64_symbolic.c", 143, __extension__ __PRETTY_FUNCTION__));
  long nondet_lMin2 = __VERIFIER_nondet_long();
  if (!(nondet_lMin2 < 0 && nondet_lMin2 >= -2L)) {
    return;
  }
  ((__builtin_popcountl(nondet_lMin2) == 64 || __builtin_popcountl(nondet_lMin2) == 63) ? (void) (0) : __assert_fail ("__builtin_popcountl(nondet_lMin2) == 64 || __builtin_popcountl(nondet_lMin2) == 63", "builtin_popcount64_symbolic.c", 149, __extension__ __PRETTY_FUNCTION__));
  unsigned long nondet_ulMax = __VERIFIER_nondet_ulong();
  if (!(nondet_ulMax > 18446744073709551614UL)) {
    return;
  }
  ((__builtin_popcountl(nondet_ulMax) == 64) ? (void) (0) : __assert_fail ("__builtin_popcountl(nondet_ulMax) == 64", "builtin_popcount64_symbolic.c", 155, __extension__ __PRETTY_FUNCTION__));
  unsigned long nondet_ulAtLeastMaxMinusOne = __VERIFIER_nondet_ulong();
  if (!(nondet_ulAtLeastMaxMinusOne > 18446744073709551613UL)) {
    return;
  }
  int count_ulAtLeastMaxMinusOne = __builtin_popcountl(nondet_ulAtLeastMaxMinusOne);
  ((count_ulAtLeastMaxMinusOne == 63 || count_ulAtLeastMaxMinusOne == 64) ? (void) (0) : __assert_fail ("count_ulAtLeastMaxMinusOne == 63 || count_ulAtLeastMaxMinusOne == 64", "builtin_popcount64_symbolic.c", 162, __extension__ __PRETTY_FUNCTION__));
}
void test_popcountll() {
  unsigned long long test_int1 = 1231;
  unsigned long long test_int2 = 0;
  unsigned long long test_int3 = 1;
  unsigned long long test_int4 = 162368;
  unsigned long long test_uint16BitMaxValue = 65535;
  unsigned long long test_uint32BitMaxValue = 4294967295UL;
  long long nondet_ll = __VERIFIER_nondet_longlong();
  if (nondet_ll != test_int1 && nondet_ll != test_int2 && nondet_ll != test_int3 && nondet_ll != test_int4 && nondet_ll != test_uint16BitMaxValue && nondet_ll != test_uint32BitMaxValue) {
    return;
  }
  int longRes1 = __builtin_popcountll(nondet_ll) == 7;
  int longRes2 = __builtin_popcountll(nondet_ll) == 0;
  int longRes3 = __builtin_popcountll(nondet_ll) == 1;
  int longRes4 = __builtin_popcountll(nondet_ll) == 7;
  int longRes5 = __builtin_popcountll(nondet_ll) == 16;
  int longRes6 = __builtin_popcountll(nondet_ll) == 32;
  ((longRes1 || longRes2 || longRes3 || longRes4 || longRes5 || longRes6) ? (void) (0) : __assert_fail ("longRes1 || longRes2 || longRes3 || longRes4 || longRes5 || longRes6", "builtin_popcount64_symbolic.c", 186, __extension__ __PRETTY_FUNCTION__));
  unsigned long long nondet_ull = __VERIFIER_nondet_ulonglong();
  if (nondet_ull != test_int1 && nondet_ull != test_int2 && nondet_ull != test_int3 && nondet_ull != test_int4 && nondet_ull != test_uint16BitMaxValue) {
    return;
  }
  int ullRes1 = __builtin_popcountll(nondet_ull) == 7;
  int ullRes2 = __builtin_popcountll(nondet_ull) == 0;
  int ullRes3 = __builtin_popcountll(nondet_ull) == 1;
  int ullRes4 = __builtin_popcountll(nondet_ull) == 7;
  int ullRes5 = __builtin_popcountll(nondet_ull) == 16;
  ((ullRes1 || ullRes2 || ullRes3 || ullRes4 || ullRes5) ? (void) (0) : __assert_fail ("ullRes1 || ullRes2 || ullRes3 || ullRes4 || ullRes5", "builtin_popcount64_symbolic.c", 199, __extension__ __PRETTY_FUNCTION__));
  long long nondet_llMax = __VERIFIER_nondet_longlong();
  if (!(nondet_llMax > 9223372036854775806LL)) {
    return;
  }
  ((__builtin_popcountll(nondet_llMax) == 63) ? (void) (0) : __assert_fail ("__builtin_popcountll(nondet_llMax) == 63", "builtin_popcount64_symbolic.c", 205, __extension__ __PRETTY_FUNCTION__));
  long long nondet_llMin = __VERIFIER_nondet_longlong();
  if (!(nondet_llMin < -9223372036854775807LL)) {
    return;
  }
  ((__builtin_popcountll(nondet_llMin) == 1) ? (void) (0) : __assert_fail ("__builtin_popcountll(nondet_llMin) == 1", "builtin_popcount64_symbolic.c", 211, __extension__ __PRETTY_FUNCTION__));
  long long nondet_llAtLeastMinPlusOne = __VERIFIER_nondet_longlong();
  if (!(nondet_llAtLeastMinPlusOne < -9223372036854775806LL)) {
    return;
  }
  int count_llAtLeastMinPlusOne = __builtin_popcountll(nondet_llAtLeastMinPlusOne);
  if (count_llAtLeastMinPlusOne == 1) {
    return;
  }
  ((count_llAtLeastMinPlusOne == 2) ? (void) (0) : __assert_fail ("count_llAtLeastMinPlusOne == 2", "builtin_popcount64_symbolic.c", 221, __extension__ __PRETTY_FUNCTION__));
  long long nondet_llMin2 = __VERIFIER_nondet_ulonglong();
  if (!(nondet_llMin2 < 0 && nondet_llMin2 >= -2LL)) {
    return;
  }
  ((__builtin_popcountll(nondet_llMin2) == 64 || __builtin_popcountll(nondet_llMin2) == 63) ? (void) (0) : __assert_fail ("__builtin_popcountll(nondet_llMin2) == 64 || __builtin_popcountll(nondet_llMin2) == 63", "builtin_popcount64_symbolic.c", 227, __extension__ __PRETTY_FUNCTION__));
  unsigned long long nondet_ullMax = __VERIFIER_nondet_ulonglong();
  if (!(nondet_ullMax > 18446744073709551614ULL)) {
    return;
  }
  ((__builtin_popcountll(nondet_ullMax) == 64) ? (void) (0) : __assert_fail ("__builtin_popcountll(nondet_ullMax) == 64", "builtin_popcount64_symbolic.c", 233, __extension__ __PRETTY_FUNCTION__));
  unsigned long long nondet_ullAtLeastMaxMinusOne = __VERIFIER_nondet_ulonglong();
  if (!(nondet_ullAtLeastMaxMinusOne > 18446744073709551613ULL)) {
    return;
  }
  int count_ullAtLeastMaxMinusOne = __builtin_popcountll(nondet_ullAtLeastMaxMinusOne);
  ((count_ullAtLeastMaxMinusOne == 63 || count_ullAtLeastMaxMinusOne == 64) ? (void) (0) : __assert_fail ("count_ullAtLeastMaxMinusOne == 63 || count_ullAtLeastMaxMinusOne == 64", "builtin_popcount64_symbolic.c", 240, __extension__ __PRETTY_FUNCTION__));
}
int main() {
  test_popcount();
  test_popcountl();
  test_popcountll();
  return 0;
}
