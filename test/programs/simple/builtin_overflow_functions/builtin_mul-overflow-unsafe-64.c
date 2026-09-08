// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


int main(void) {
  int all_expected_checks_fail = 0;


  // Fixed typed test values are initialized directly and never modified.
  const char char_max = 127;
  const signed char schar_min = -128;
  const signed char schar_max = 127;
  const unsigned char uchar_max = 255U;
  const short int short_max = 32767;
  const int int_min = (-2147483647 - 1);
  const int int_max = 2147483647;
  const unsigned int uint_max = 4294967295U;
  const long long int ll_min = (-9223372036854775807LL - 1LL);
  const long long int ll_max = 9223372036854775807LL;
  const unsigned long long int ull_max = 18446744073709551615ULL;
  const char char_2 = -2;


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: 2147483647L * 2L fits in long, stores 4294967294L, and returns 0.
  const long long int model_smull_expected_res = 4294967294LL;
  long int model_smull_res;
  int model_smull_ov = __builtin_smull_overflow(2147483647L, 2L, &model_smull_res);
  all_expected_checks_fail =
    all_expected_checks_fail || ((long long int)model_smull_res != model_smull_expected_res);
  all_expected_checks_fail = all_expected_checks_fail || (model_smull_ov != 0);

  // Generic multiplication overflow tests.

  const unsigned long int mul_ulong_max = ~0UL;


  const long int mul_long_max = (long int)((~0UL) >> 1);


  signed char mul_res_1;
  int mul_ov_1 = __builtin_mul_overflow(schar_max, 1, &mul_res_1);

  // 127 * 1 = 127, which fits the destination range; stored result = 127 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_1 != schar_max);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_1 != 0);


  signed char mul_res_2;
  int mul_ov_2 = __builtin_mul_overflow(schar_min, -1, &mul_res_2);

  // -128 * -1 = 128, outside the destination range; stored result = -128 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_2 != schar_min);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_2 != 1);


  unsigned char mul_res_3;
  int mul_ov_3 = __builtin_mul_overflow(uchar_max, uchar_max, &mul_res_3);

  // 255U * 255U = 65025, outside the destination range; stored result = 1U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_3 != 1U);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_3 != 1);


  short int mul_res_4;
  int mul_ov_4 = __builtin_mul_overflow(short_max, 2, &mul_res_4);

  // 32767 * 2 = 65534, outside the destination range; stored result = -2 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_4 != -2);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_4 != 1);


  unsigned short int mul_res_5;
  int mul_ov_5 = __builtin_mul_overflow(2U, 32767U, &mul_res_5);

  // 2U * 32767U = 65534, which fits the destination range; stored result = 65534U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_5 != 65534U);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_5 != 0);


  int mul_res_6;
  int mul_ov_6 = __builtin_mul_overflow(int_min, -1, &mul_res_6);

  // -2147483648 * -1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_6 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_6 != 1);


  unsigned int mul_res_7;
  int mul_ov_7 = __builtin_mul_overflow(uint_max, uint_max, &mul_res_7);

  // 4294967295U * 4294967295U = 18446744065119617025, outside the destination range; stored result = 1U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_7 != 1U);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_7 != 1);


  long int mul_res_8;
  int mul_ov_8 = __builtin_mul_overflow(mul_long_max, 0L, &mul_res_8);

  // 9223372036854775807L * 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_8 != 0L);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_8 != 0);


  unsigned long int mul_res_9;
  int mul_ov_9 = __builtin_mul_overflow(mul_ulong_max, 2UL, &mul_res_9);

  // 18446744073709551615UL * 2UL = 36893488147419103230, outside the destination range; stored result = 18446744073709551614UL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_9 != (~0UL) - 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_9 != 1);


  long long int mul_res_10;
  int mul_ov_10 = __builtin_mul_overflow(ll_min, 0LL, &mul_res_10);

  // -9223372036854775808LL * 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_10 != 0LL);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_10 != 0);


  unsigned long long int mul_res_11;
  int mul_ov_11 = __builtin_mul_overflow(ull_max, ull_max, &mul_res_11);

  // 18446744073709551615ULL * 18446744073709551615ULL = 340282366920938463426481119284349108225, outside the destination range; stored
  // result = 1ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_11 != 1ULL);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_11 != 1);


  char mul_res_12;
  int mul_ov_12 = __builtin_mul_overflow(char_max, 2, &mul_res_12);

  // (char)127 * 2 = 254, outside the destination range; stored result = (char)-2 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_12 != char_2);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_12 != 1);


  int mul_res_13;
  int mul_ov_13 = __builtin_mul_overflow(-1LL, 1ULL, &mul_res_13);

  // -1LL * 1ULL = -1, which fits the destination range; stored result = -1 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_13 != -1);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_13 != 0);


  unsigned int mul_res_14;
  int mul_ov_14 = __builtin_mul_overflow(-1LL, 2ULL, &mul_res_14);

  // -1LL * 2ULL = -2, outside the destination range; stored result = 4294967294U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_res_14 != 4294967294U);

  all_expected_checks_fail = all_expected_checks_fail || (mul_ov_14 != 1);


  // Signed int multiplication overflow tests.

  int smul_res_1;
  int smul_ov_1 = __builtin_smul_overflow(0, 0, &smul_res_1);

  // 0 * 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_1 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_1 != 0);


  int smul_res_2;
  int smul_ov_2 = __builtin_smul_overflow(int_max, 0, &smul_res_2);

  // 2147483647 * 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_2 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_2 != 0);


  int smul_res_3;
  int smul_ov_3 = __builtin_smul_overflow(int_min, 0, &smul_res_3);

  // -2147483648 * 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_3 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_3 != 0);


  int smul_res_4;
  int smul_ov_4 = __builtin_smul_overflow(int_max, 1, &smul_res_4);

  // 2147483647 * 1 = 2147483647, which fits the destination range; stored result = 2147483647 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_4 != int_max);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_4 != 0);


  int smul_res_5;
  int smul_ov_5 = __builtin_smul_overflow(int_min, 1, &smul_res_5);

  // -2147483648 * 1 = -2147483648, which fits the destination range; stored result = -2147483648 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_5 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_5 != 0);


  int smul_res_6;
  int smul_ov_6 = __builtin_smul_overflow(int_max, -1, &smul_res_6);

  // 2147483647 * -1 = -2147483647, which fits the destination range; stored result = -2147483647 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_6 != -int_max);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_6 != 0);


  int smul_res_7;
  int smul_ov_7 = __builtin_smul_overflow(int_min, -1, &smul_res_7);

  // -2147483648 * -1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_7 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_7 != 1);


  int smul_res_8;
  int smul_ov_8 = __builtin_smul_overflow(int_max, 2, &smul_res_8);

  // 2147483647 * 2 = 4294967294, outside the destination range; stored result = -2 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_8 != -2);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_8 != 1);


  int smul_res_9;
  int smul_ov_9 = __builtin_smul_overflow(int_min, 2, &smul_res_9);

  // -2147483648 * 2 = -4294967296, outside the destination range; stored result = 0 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_9 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_9 != 1);


  int smul_res_10;
  int smul_ov_10 = __builtin_smul_overflow(-1, -1, &smul_res_10);

  // -1 * -1 = 1, which fits the destination range; stored result = 1 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smul_res_10 != 1);

  all_expected_checks_fail = all_expected_checks_fail || (smul_ov_10 != 0);


  // Signed long int multiplication overflow tests.


  const long int smull_max = (long int)((~0UL) >> 1);


  const long int smull_min = (-((long int)((~0UL) >> 1)) - 1L);


  long int smull_res_1;
  int smull_ov_1 = __builtin_smull_overflow(0L, 0L, &smull_res_1);

  // 0L * 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_1 != 0L);

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_1 != 0);


  long int smull_res_2;
  int smull_ov_2 = __builtin_smull_overflow(smull_max, 0L, &smull_res_2);

  // 9223372036854775807L * 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_2 != 0L);

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_2 != 0);


  long int smull_res_3;
  int smull_ov_3 = __builtin_smull_overflow(smull_min, 0L, &smull_res_3);

  // -9223372036854775808L * 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_3 != 0L);

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_3 != 0);


  long int smull_res_4;
  int smull_ov_4 = __builtin_smull_overflow(smull_max, 1L, &smull_res_4);

  // 9223372036854775807L * 1L = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807L and overflow =
  // 0.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_4 != smull_max);

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_4 != 0);


  long int smull_res_5;
  int smull_ov_5 = __builtin_smull_overflow(smull_min, 1L, &smull_res_5);

  // -9223372036854775808L * 1L = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808L and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_5 != smull_min);

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_5 != 0);


  long int smull_res_6;
  int smull_ov_6 = __builtin_smull_overflow(smull_max, -1L, &smull_res_6);

  // 9223372036854775807L * -1L = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807L and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_6 != - ((long int)((~0UL) >> 1)));

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_6 != 0);


  long int smull_res_7;
  int smull_ov_7 = __builtin_smull_overflow(smull_min, -1L, &smull_res_7);

  // -9223372036854775808L * -1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_7 != smull_min);

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_7 != 1);


  long int smull_res_8;
  int smull_ov_8 = __builtin_smull_overflow(smull_max, 2L, &smull_res_8);

  // 9223372036854775807L * 2L = 18446744073709551614, outside the destination range; stored result = -2L and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_8 != -2L);

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_8 != 1);


  long int smull_res_9;
  int smull_ov_9 = __builtin_smull_overflow(smull_min, 2L, &smull_res_9);

  // -9223372036854775808L * 2L = -18446744073709551616, outside the destination range; stored result = 0L and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_9 != 0L);

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_9 != 1);


  long int smull_res_10;
  int smull_ov_10 = __builtin_smull_overflow(-1L, -1L, &smull_res_10);

  // -1L * -1L = 1, which fits the destination range; stored result = 1L and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smull_res_10 != 1L);

  all_expected_checks_fail = all_expected_checks_fail || (smull_ov_10 != 0);


  // Signed long long int multiplication overflow tests.

  long long int smulll_res_1;
  int smulll_ov_1 = __builtin_smulll_overflow(0LL, 0LL, &smulll_res_1);

  // 0LL * 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_1 != 0LL);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_1 != 0);


  long long int smulll_res_2;
  int smulll_ov_2 = __builtin_smulll_overflow(ll_max, 0LL, &smulll_res_2);

  // 9223372036854775807LL * 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_2 != 0LL);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_2 != 0);


  long long int smulll_res_3;
  int smulll_ov_3 = __builtin_smulll_overflow(ll_min, 0LL, &smulll_res_3);

  // -9223372036854775808LL * 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_3 != 0LL);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_3 != 0);


  long long int smulll_res_4;
  int smulll_ov_4 = __builtin_smulll_overflow(ll_max, 1LL, &smulll_res_4);

  // 9223372036854775807LL * 1LL = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807LL and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_4 != ll_max);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_4 != 0);


  long long int smulll_res_5;
  int smulll_ov_5 = __builtin_smulll_overflow(ll_min, 1LL, &smulll_res_5);

  // -9223372036854775808LL * 1LL = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_5 != ll_min);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_5 != 0);


  long long int smulll_res_6;
  int smulll_ov_6 = __builtin_smulll_overflow(ll_max, -1LL, &smulll_res_6);

  // 9223372036854775807LL * -1LL = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_6 != -ll_max);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_6 != 0);


  long long int smulll_res_7;
  int smulll_ov_7 = __builtin_smulll_overflow(ll_min, -1LL, &smulll_res_7);

  // -9223372036854775808LL * -1LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_7 != ll_min);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_7 != 1);


  long long int smulll_res_8;
  int smulll_ov_8 = __builtin_smulll_overflow(ll_max, 2LL, &smulll_res_8);

  // 9223372036854775807LL * 2LL = 18446744073709551614, outside the destination range; stored result = -2LL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_8 != -2LL);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_8 != 1);


  long long int smulll_res_9;
  int smulll_ov_9 = __builtin_smulll_overflow(ll_min, 2LL, &smulll_res_9);

  // -9223372036854775808LL * 2LL = -18446744073709551616, outside the destination range; stored result = 0LL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_9 != 0LL);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_9 != 1);


  long long int smulll_res_10;
  int smulll_ov_10 = __builtin_smulll_overflow(-1LL, -1LL, &smulll_res_10);

  // -1LL * -1LL = 1, which fits the destination range; stored result = 1LL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (smulll_res_10 != 1LL);

  all_expected_checks_fail = all_expected_checks_fail || (smulll_ov_10 != 0);


  // Unsigned int multiplication overflow tests.

  unsigned int umul_res_1;
  int umul_ov_1 = __builtin_umul_overflow(0U, uint_max, &umul_res_1);

  // 0U * 4294967295U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (umul_res_1 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (umul_ov_1 != 0);


  unsigned int umul_res_2;
  int umul_ov_2 = __builtin_umul_overflow(1U, uint_max, &umul_res_2);

  // 1U * 4294967295U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (umul_res_2 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (umul_ov_2 != 0);


  unsigned int umul_res_3;
  int umul_ov_3 = __builtin_umul_overflow(2U, 2147483647U, &umul_res_3);

  // 2U * 2147483647U = 4294967294, which fits the destination range; stored result = 4294967294U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (umul_res_3 != 4294967294U);

  all_expected_checks_fail = all_expected_checks_fail || (umul_ov_3 != 0);


  unsigned int umul_res_4;
  int umul_ov_4 = __builtin_umul_overflow(2U, 2147483648U, &umul_res_4);

  // 2U * 2147483648U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (umul_res_4 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (umul_ov_4 != 1);


  unsigned int umul_res_5;
  int umul_ov_5 = __builtin_umul_overflow(uint_max, uint_max, &umul_res_5);

  // 4294967295U * 4294967295U = 18446744065119617025, outside the destination range; stored result = 1U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (umul_res_5 != 1U);

  all_expected_checks_fail = all_expected_checks_fail || (umul_ov_5 != 1);


  unsigned int umul_res_6;
  int umul_ov_6 = __builtin_umul_overflow(uint_max, 2U, &umul_res_6);

  // 4294967295U * 2U = 8589934590, outside the destination range; stored result = 4294967294U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (umul_res_6 != 4294967294U);

  all_expected_checks_fail = all_expected_checks_fail || (umul_ov_6 != 1);


  // Unsigned long int multiplication overflow tests.

  const unsigned long int umull_max = ~0UL;


  unsigned long int umull_res_1;
  int umull_ov_1 = __builtin_umull_overflow(0UL, umull_max, &umull_res_1);

  // 0UL * 18446744073709551615UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (umull_res_1 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (umull_ov_1 != 0);


  unsigned long int umull_res_2;
  int umull_ov_2 = __builtin_umull_overflow(1UL, umull_max, &umull_res_2);

  // 1UL * 18446744073709551615UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (umull_res_2 != umull_max);

  all_expected_checks_fail = all_expected_checks_fail || (umull_ov_2 != 0);


  unsigned long int umull_res_3;
  int umull_ov_3 = __builtin_umull_overflow(2UL, (~0UL) / 2UL, &umull_res_3);

  // 2UL * 9223372036854775807UL = 18446744073709551614, which fits the destination range; stored result = 18446744073709551614UL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (umull_res_3 != (~0UL) - 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (umull_ov_3 != 0);


  unsigned long int umull_res_4;
  int umull_ov_4 = __builtin_umull_overflow(2UL, (~0UL) / 2UL + 1UL, &umull_res_4);

  // 2UL * 9223372036854775808UL = 18446744073709551616, outside the destination range; stored result = 0UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (umull_res_4 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (umull_ov_4 != 1);


  unsigned long int umull_res_5;
  int umull_ov_5 = __builtin_umull_overflow(umull_max, umull_max, &umull_res_5);

  // 18446744073709551615UL * 18446744073709551615UL = 340282366920938463426481119284349108225, outside the destination range; stored result
  // = 1UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (umull_res_5 != 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (umull_ov_5 != 1);


  unsigned long int umull_res_6;
  int umull_ov_6 = __builtin_umull_overflow(umull_max, 2UL, &umull_res_6);

  // 18446744073709551615UL * 2UL = 36893488147419103230, outside the destination range; stored result = 18446744073709551614UL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (umull_res_6 != (~0UL) - 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (umull_ov_6 != 1);


  // Unsigned long long int multiplication overflow tests.

  unsigned long long int umulll_res_1;
  int umulll_ov_1 = __builtin_umulll_overflow(0ULL, ull_max, &umulll_res_1);

  // 0ULL * 18446744073709551615ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (umulll_res_1 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (umulll_ov_1 != 0);


  unsigned long long int umulll_res_2;
  int umulll_ov_2 = __builtin_umulll_overflow(1ULL, ull_max, &umulll_res_2);

  // 1ULL * 18446744073709551615ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (umulll_res_2 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (umulll_ov_2 != 0);


  unsigned long long int umulll_res_3;
  int umulll_ov_3 = __builtin_umulll_overflow(2ULL, 9223372036854775807ULL, &umulll_res_3);

  // 2ULL * 9223372036854775807ULL = 18446744073709551614, which fits the destination range; stored result = 18446744073709551614ULL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (umulll_res_3 != 18446744073709551614ULL);

  all_expected_checks_fail = all_expected_checks_fail || (umulll_ov_3 != 0);


  unsigned long long int umulll_res_4;
  int umulll_ov_4 = __builtin_umulll_overflow(2ULL, 9223372036854775808ULL, &umulll_res_4);

  // 2ULL * 9223372036854775808ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (umulll_res_4 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (umulll_ov_4 != 1);


  unsigned long long int umulll_res_5;
  int umulll_ov_5 = __builtin_umulll_overflow(ull_max, ull_max, &umulll_res_5);

  // 18446744073709551615ULL * 18446744073709551615ULL = 340282366920938463426481119284349108225, outside the destination range; stored
  // result = 1ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (umulll_res_5 != 1ULL);

  all_expected_checks_fail = all_expected_checks_fail || (umulll_ov_5 != 1);


  unsigned long long int umulll_res_6;
  int umulll_ov_6 = __builtin_umulll_overflow(ull_max, 2ULL, &umulll_res_6);

  // 18446744073709551615ULL * 2ULL = 36893488147419103230, outside the destination range; stored result = 18446744073709551614ULL and
  // overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (umulll_res_6 != 18446744073709551614ULL);

  all_expected_checks_fail = all_expected_checks_fail || (umulll_ov_6 != 1);


  // If no expected-value check fails, every deliberately negated check is false.
  if (!(all_expected_checks_fail))
    goto ERROR;

  return 0;

ERROR:
  return 1;
}
