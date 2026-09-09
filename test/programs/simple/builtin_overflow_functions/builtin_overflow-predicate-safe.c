// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}


int main(void) {

  // Constant test values are initialized directly and never modified.
  const char char_min = -128;
  const char char_max = 127;
  const signed char schar_min = -128;
  const signed char schar_max = 127;
  const unsigned char uchar_max = 255U;
  const short int short_min = -32768;
  const short int short_max = 32767;
  const unsigned short int ushort_max = 65535U;
  const int int_min = (-2147483647 - 1);
  const int int_max = 2147483647;
  const unsigned int uint_max = 4294967295U;
  const long long int ll_min = (-9223372036854775807LL - 1LL);
  const unsigned long long int ull_max = 18446744073709551615ULL;
  const signed char schar_0 = 0;
  const unsigned char uchar_0 = 0U;
  const short int short_0 = 0;
  const unsigned short int ushort_0 = 0U;
  const int int_0 = 0;
  const unsigned int uint_0 = 0U;
  const long int long_0 = 0L;
  const unsigned long int ulong_0 = 0UL;
  const long long int ll_0 = 0LL;
  const unsigned long long int ull_0 = 0ULL;
  const char char_0 = 0;
  const unsigned long int addp_ulong_max = ~0UL;
  const long int addp_long_max = (long int)((~0UL) >> 1);
  const unsigned long int subp_ulong_max = ~0UL;
  const long int subp_long_max = (long int)((~0UL) >> 1);
  const unsigned long int mulp_ulong_max = ~0UL;
  const long int mulp_long_max = (long int)((~0UL) >> 1);


  // long is 32 bits in ILP32 and 64 bits in LP64, so this builtin case has model-dependent expected results.
  if (sizeof(long int) == 4U) {
    int model_addp = __builtin_add_overflow_p(2147483647L, 1L, (long int)0);

    // ILP32: 2147483647L + 1L = 2147483648, which is outside long, so the predicate returns 1.
    __VERIFIER_assert(model_addp == 1);
  } else {
    int model_addp = __builtin_add_overflow_p(2147483647L, 1L, (long int)0);

    // LP64: 2147483647L + 1L = 2147483648L, which fits long, so the predicate returns 0.
    __VERIFIER_assert(model_addp == 0);
  }

  int addp_schar_max_0;
  addp_schar_max_0 = __builtin_add_overflow_p(schar_max, 0, schar_0);

  // 127 + 0 = 127, which fits the destination range; overflow = 0.
  __VERIFIER_assert(addp_schar_max_0 == 0);


  int addp_schar_max_1;
  addp_schar_max_1 = __builtin_add_overflow_p(schar_max, 1, schar_0);

  // 127 + 1 = 128, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(addp_schar_max_1 == 1);


  int addp_uchar_max_1;
  addp_uchar_max_1 = __builtin_add_overflow_p(uchar_max, 1, uchar_0);

  // 255U + 1 = 256, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(addp_uchar_max_1 == 1);


  int addp_short_min_m1;
  addp_short_min_m1 = __builtin_add_overflow_p(short_min, -1, short_0);

  // -32768 + -1 = -32769, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(addp_short_min_m1 == 1);


  int addp_ushort_m1_1;
  addp_ushort_m1_1 = __builtin_add_overflow_p(-1, 1U, ushort_0);

  // -1 + 1U = 0, which fits the destination range; overflow = 0.
  __VERIFIER_assert(addp_ushort_m1_1 == 0);


  int addp_int_max_1;
  addp_int_max_1 = __builtin_add_overflow_p(int_max, 1, int_0);

  // 2147483647 + 1 = 2147483648, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(addp_int_max_1 == 1);


  int addp_uint_max_1;
  addp_uint_max_1 = __builtin_add_overflow_p(uint_max, 1U, uint_0);

  // 4294967295U + 1U = 4294967296, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(addp_uint_max_1 == 1);


  int addp_long_max_0;
  addp_long_max_0 = __builtin_add_overflow_p(addp_long_max, 0L, long_0);

  // ILP32: 2147483647L + 0L = 2147483647, which fits the destination range; overflow = 0.
  // LP64: 9223372036854775807L + 0L = 9223372036854775807, which fits the destination range; overflow = 0.
  __VERIFIER_assert(addp_long_max_0 == 0);


  int addp_ulong_max_1;
  addp_ulong_max_1 = __builtin_add_overflow_p(addp_ulong_max, 1UL, ulong_0);

  // ILP32: 4294967295UL + 1UL = 4294967296, which is outside the destination range; overflow = 1.
  // LP64: 18446744073709551615UL + 1UL = 18446744073709551616, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(addp_ulong_max_1 == 1);


  int addp_ll_min_m1;
  addp_ll_min_m1 = __builtin_add_overflow_p(ll_min, -1LL, ll_0);

  // -9223372036854775808LL + -1LL = -9223372036854775809, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(addp_ll_min_m1 == 1);


  int addp_ull_max_0;
  addp_ull_max_0 = __builtin_add_overflow_p(ull_max, 0ULL, ull_0);

  // 18446744073709551615ULL + 0ULL = 18446744073709551615, which fits the destination range; overflow = 0.
  __VERIFIER_assert(addp_ull_max_0 == 0);


  int addp_char_max_1;
  addp_char_max_1 = __builtin_add_overflow_p(char_max, 1, char_0);

  // 127 + 1 = 128, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(addp_char_max_1 == 1);
  int subp_schar_min_0;
  subp_schar_min_0 = __builtin_sub_overflow_p(schar_min, 0, schar_0);

  // -128 - 0 = -128, which fits the destination range; overflow = 0.
  __VERIFIER_assert(subp_schar_min_0 == 0);


  int subp_schar_min_1;
  subp_schar_min_1 = __builtin_sub_overflow_p(schar_min, 1, schar_0);

  // -128 - 1 = -129, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(subp_schar_min_1 == 1);


  int subp_0_1;
  subp_0_1 = __builtin_sub_overflow_p(0, 1, uchar_0);

  // 0 - 1 = -1, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(subp_0_1 == 1);


  int subp_short_max_m1;
  subp_short_max_m1 = __builtin_sub_overflow_p(short_max, -1, short_0);

  // 32767 - -1 = 32768, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(subp_short_max_m1 == 1);


  int subp_ushort_max_max;
  subp_ushort_max_max = __builtin_sub_overflow_p(ushort_max, ushort_max, ushort_0);

  // 65535U - 65535U = 0, which fits the destination range; overflow = 0.
  __VERIFIER_assert(subp_ushort_max_max == 0);


  int subp_int_min_1;
  subp_int_min_1 = __builtin_sub_overflow_p(int_min, 1, int_0);

  // -2147483648 - 1 = -2147483649, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(subp_int_min_1 == 1);


  int subp_uint_0_max;
  subp_uint_0_max = __builtin_sub_overflow_p(0U, uint_max, uint_0);

  // 0U - 4294967295U = -4294967295, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(subp_uint_0_max == 1);


  int subp_long_max_m1;
  subp_long_max_m1 = __builtin_sub_overflow_p(subp_long_max, -1L, long_0);

  // ILP32: 2147483647L - -1L = 2147483648, which is outside the destination range; overflow = 1.
  // LP64: 9223372036854775807L - -1L = 9223372036854775808, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(subp_long_max_m1 == 1);


  int subp_ulong_max_max;
  subp_ulong_max_max = __builtin_sub_overflow_p(subp_ulong_max, subp_ulong_max, ulong_0);

  // ILP32: 4294967295UL - 4294967295UL = 0, which fits the destination range; overflow = 0.
  // LP64: 18446744073709551615UL - 18446744073709551615UL = 0, which fits the destination range; overflow = 0.
  __VERIFIER_assert(subp_ulong_max_max == 0);


  int subp_ll_0_min;
  subp_ll_0_min = __builtin_sub_overflow_p(0LL, ll_min, ll_0);

  // 0LL - -9223372036854775808LL = 9223372036854775808, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(subp_ll_0_min == 1);


  int subp_ull_max_max;
  subp_ull_max_max = __builtin_sub_overflow_p(ull_max, ull_max, ull_0);

  // 18446744073709551615ULL - 18446744073709551615ULL = 0, which fits the destination range; overflow = 0.
  __VERIFIER_assert(subp_ull_max_max == 0);


  int subp_char_min_1;
  subp_char_min_1 = __builtin_sub_overflow_p(char_min, 1, char_0);

  // -128 - 1 = -129, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(subp_char_min_1 == 1);
  int mulp_schar_max_1;
  mulp_schar_max_1 = __builtin_mul_overflow_p(schar_max, 1, schar_0);

  // 127 * 1 = 127, which fits the destination range; overflow = 0.
  __VERIFIER_assert(mulp_schar_max_1 == 0);


  int mulp_schar_min_m1;
  mulp_schar_min_m1 = __builtin_mul_overflow_p(schar_min, -1, schar_0);

  // -128 * -1 = 128, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(mulp_schar_min_m1 == 1);


  int mulp_uchar_max_max;
  mulp_uchar_max_max = __builtin_mul_overflow_p(uchar_max, uchar_max, uchar_0);

  // 255U * 255U = 65025, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(mulp_uchar_max_max == 1);


  int mulp_short_max_0;
  mulp_short_max_0 = __builtin_mul_overflow_p(short_max, 0, short_0);

  // 32767 * 0 = 0, which fits the destination range; overflow = 0.
  __VERIFIER_assert(mulp_short_max_0 == 0);


  int mulp_ushort_2_32768;
  mulp_ushort_2_32768 = __builtin_mul_overflow_p(2U, 32768U, ushort_0);

  // 2U * 32768U = 65536, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(mulp_ushort_2_32768 == 1);


  int mulp_int_min_m1;
  mulp_int_min_m1 = __builtin_mul_overflow_p(int_min, -1, int_0);

  // -2147483648 * -1 = 2147483648, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(mulp_int_min_m1 == 1);


  int mulp_uint_max_max;
  mulp_uint_max_max = __builtin_mul_overflow_p(uint_max, uint_max, uint_0);

  // 4294967295U * 4294967295U = 18446744065119617025, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(mulp_uint_max_max == 1);


  int mulp_long_max_0;
  mulp_long_max_0 = __builtin_mul_overflow_p(mulp_long_max, 0L, long_0);

  // ILP32: 2147483647L * 0L = 0, which fits the destination range; overflow = 0.
  // LP64: 9223372036854775807L * 0L = 0, which fits the destination range; overflow = 0.
  __VERIFIER_assert(mulp_long_max_0 == 0);


  int mulp_ulong_max_2;
  mulp_ulong_max_2 = __builtin_mul_overflow_p(mulp_ulong_max, 2UL, ulong_0);

  // ILP32: 4294967295UL * 2UL = 8589934590, which is outside the destination range; overflow = 1.
  // LP64: 18446744073709551615UL * 2UL = 36893488147419103230, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(mulp_ulong_max_2 == 1);


  int mulp_ll_min_1;
  mulp_ll_min_1 = __builtin_mul_overflow_p(ll_min, 1LL, ll_0);

  // -9223372036854775808LL * 1LL = -9223372036854775808, which fits the destination range; overflow = 0.
  __VERIFIER_assert(mulp_ll_min_1 == 0);


  int mulp_ull_max_max;
  mulp_ull_max_max = __builtin_mul_overflow_p(ull_max, ull_max, ull_0);

  // 18446744073709551615ULL * 18446744073709551615ULL = 340282366920938463426481119284349108225, which is outside the destination range;
  // overflow = 1.
  __VERIFIER_assert(mulp_ull_max_max == 1);


  int mulp_char_max_2;
  mulp_char_max_2 = __builtin_mul_overflow_p(char_max, 2, char_0);

  // 127 * 2 = 254, which is outside the destination range; overflow = 1.
  __VERIFIER_assert(mulp_char_max_2 == 1);


  return 0;
}
