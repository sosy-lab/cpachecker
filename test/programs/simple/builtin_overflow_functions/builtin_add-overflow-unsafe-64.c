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
  const char char_min = -128;
  const char char_max = 127;
  const signed char schar_min = -128;
  const signed char schar_max = 127;
  const unsigned char uchar_max = 255U;
  const short int short_min = -32768;
  const short int short_max = 32767;
  const int int_min = (-2147483647 - 1);
  const int int_max = 2147483647;
  const unsigned int uint_max = 4294967295U;
  const long long int ll_min = (-9223372036854775807LL - 1LL);
  const long long int ll_max = 9223372036854775807LL;
  const unsigned long long int ull_max = 18446744073709551615ULL;


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: 2147483647L + 1L = 2147483648, within long range; result = 2147483648L and overflow = 0.
  const long long int model_saddl_expected_res = 2147483648LL;
  long int model_saddl_res;
  int model_saddl_ov = __builtin_saddl_overflow(2147483647L, 1L, &model_saddl_res);
  all_expected_checks_fail =
    all_expected_checks_fail || ((long long int)model_saddl_res != model_saddl_expected_res);
  all_expected_checks_fail = all_expected_checks_fail || (model_saddl_ov != 0);

  // Generic addition overflow tests.

  const unsigned long int add_ulong_max = ~0UL;


  const long int add_long_max = (long int)((~0UL) >> 1);


  const long int add_long_min = (-((long int)((~0UL) >> 1)) - 1L);


  signed char add_res_1;
  int add_ov_1 = __builtin_add_overflow(schar_max, 0, &add_res_1);

  // 127 + 0 = 127, which fits the destination range; stored result = 127 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_1 != schar_max);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_1 != 0);


  signed char add_res_2;
  int add_ov_2 = __builtin_add_overflow(schar_max, 1, &add_res_2);

  // 127 + 1 = 128, outside the destination range; stored result = -128 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_2 != schar_min);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_2 != 1);


  unsigned char add_res_3;
  int add_ov_3 = __builtin_add_overflow(uchar_max, 1, &add_res_3);

  // 255U + 1 = 256, outside the destination range; stored result = 0 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_3 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_3 != 1);


  short int add_res_4;
  int add_ov_4 = __builtin_add_overflow(short_min, -1, &add_res_4);

  // -32768 + -1 = -32769, outside the destination range; stored result = 32767 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_4 != short_max);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_4 != 1);


  unsigned short int add_res_5;
  int add_ov_5 = __builtin_add_overflow(-1, 1U, &add_res_5);

  // -1 + 1U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_5 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_5 != 0);


  int add_res_6;
  int add_ov_6 = __builtin_add_overflow(int_max, 1, &add_res_6);

  // 2147483647 + 1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_6 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_6 != 1);


  unsigned int add_res_7;
  int add_ov_7 = __builtin_add_overflow(uint_max, 1U, &add_res_7);

  // 4294967295U + 1U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_7 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_7 != 1);


  long int add_res_8;
  int add_ov_8 = __builtin_add_overflow(add_long_max, 1L, &add_res_8);

  // 9223372036854775807L + 1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_8 != add_long_min);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_8 != 1);


  unsigned long int add_res_9;
  int add_ov_9 = __builtin_add_overflow(add_ulong_max, 1UL, &add_res_9);

  // 18446744073709551615UL + 1UL = 18446744073709551616, outside the destination range; stored result = 0UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_9 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_9 != 1);


  long long int add_res_10;
  int add_ov_10 = __builtin_add_overflow(ll_min, -1LL, &add_res_10);

  // -9223372036854775808LL + -1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_10 != ll_max);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_10 != 1);


  unsigned long long int add_res_11;
  int add_ov_11 = __builtin_add_overflow(ull_max, 1ULL, &add_res_11);

  // 18446744073709551615ULL + 1ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_11 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_11 != 1);


  char add_res_12;
  int add_ov_12 = __builtin_add_overflow(char_max, 1, &add_res_12);

  // (char)127 + 1 = 128, outside the destination range; stored result = (char)-128 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_12 != char_min);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_12 != 1);


  int add_res_13;
  int add_ov_13 = __builtin_add_overflow(-1LL, 1ULL, &add_res_13);

  // -1LL + 1ULL = 0, which fits the destination range; stored result = 0 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_13 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_13 != 0);


  int add_res_14;
  int add_ov_14 = __builtin_add_overflow(ull_max, 0, &add_res_14);

  // 18446744073709551615ULL + 0 = 18446744073709551615, outside the destination range; stored result = (int)18446744073709551615ULL and
  // overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_res_14 != (int)ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (add_ov_14 != 1);


  // Signed int addition overflow tests.

  int sadd_res_1;
  int sadd_ov_1 = __builtin_sadd_overflow(0, 0, &sadd_res_1);

  // 0 + 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_res_1 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_ov_1 != 0);


  int sadd_res_2;
  int sadd_ov_2 = __builtin_sadd_overflow(int_max, 0, &sadd_res_2);

  // 2147483647 + 0 = 2147483647, which fits the destination range; stored result = 2147483647 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_res_2 != int_max);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_ov_2 != 0);


  int sadd_res_3;
  int sadd_ov_3 = __builtin_sadd_overflow(int_min, 0, &sadd_res_3);

  // -2147483648 + 0 = -2147483648, which fits the destination range; stored result = -2147483648 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_res_3 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_ov_3 != 0);


  int sadd_res_4;
  int sadd_ov_4 = __builtin_sadd_overflow(int_max, -1, &sadd_res_4);

  // 2147483647 + -1 = 2147483646, which fits the destination range; stored result = 2147483646 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_res_4 != 2147483646);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_ov_4 != 0);


  int sadd_res_5;
  int sadd_ov_5 = __builtin_sadd_overflow(int_min, 1, &sadd_res_5);

  // -2147483648 + 1 = -2147483647, which fits the destination range; stored result = -2147483647 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_res_5 != -int_max);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_ov_5 != 0);


  int sadd_res_6;
  int sadd_ov_6 = __builtin_sadd_overflow(int_max, 1, &sadd_res_6);

  // 2147483647 + 1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_res_6 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_ov_6 != 1);


  int sadd_res_7;
  int sadd_ov_7 = __builtin_sadd_overflow(int_min, -1, &sadd_res_7);

  // -2147483648 + -1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_res_7 != int_max);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_ov_7 != 1);


  // Signed long int addition overflow tests.


  const long int saddl_max = (long int)((~0UL) >> 1);


  const long int saddl_min = (-((long int)((~0UL) >> 1)) - 1L);


  long int saddl_res_1;
  int saddl_ov_1 = __builtin_saddl_overflow(0L, 0L, &saddl_res_1);

  // 0L + 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_res_1 != 0L);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_ov_1 != 0);


  long int saddl_res_2;
  int saddl_ov_2 = __builtin_saddl_overflow(saddl_max, 0L, &saddl_res_2);

  // 9223372036854775807L + 0L = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807L and overflow =
  // 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_res_2 != saddl_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_ov_2 != 0);


  long int saddl_res_3;
  int saddl_ov_3 = __builtin_saddl_overflow(saddl_min, 0L, &saddl_res_3);

  // -9223372036854775808L + 0L = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808L and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_res_3 != saddl_min);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_ov_3 != 0);


  long int saddl_res_4;
  int saddl_ov_4 = __builtin_saddl_overflow(saddl_max, -1L, &saddl_res_4);

  // 9223372036854775807L + -1L = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806L and overflow =
  // 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_res_4 != ((long int)((~0UL) >> 1)) - 1L);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_ov_4 != 0);


  long int saddl_res_5;
  int saddl_ov_5 = __builtin_saddl_overflow(saddl_min, 1L, &saddl_res_5);

  // saddl_min + 1L = saddl_min + 1L, which fits the destination range; stored result = -9223372036854775807L
  // and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_res_5 != ((-((long int)((~0UL) >> 1)) - 1L)) + 1L);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_ov_5 != 0);


  long int saddl_res_6;
  int saddl_ov_6 = __builtin_saddl_overflow(saddl_max, 1L, &saddl_res_6);

  // 9223372036854775807L + 1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_res_6 != saddl_min);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_ov_6 != 1);


  long int saddl_res_7;
  int saddl_ov_7 = __builtin_saddl_overflow(saddl_min, -1L, &saddl_res_7);

  // -9223372036854775808L + -1L = -9223372036854775809, outside the destination range; stored result = 9223372036854775807L and overflow =
  // 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_res_7 != saddl_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_ov_7 != 1);


  // Signed long long int addition overflow tests.

  long long int saddll_res_1;
  int saddll_ov_1 = __builtin_saddll_overflow(0LL, 0LL, &saddll_res_1);

  // 0LL + 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_res_1 != 0LL);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_ov_1 != 0);


  long long int saddll_res_2;
  int saddll_ov_2 = __builtin_saddll_overflow(ll_max, 0LL, &saddll_res_2);

  // 9223372036854775807LL + 0LL = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807LL and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_res_2 != ll_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_ov_2 != 0);


  long long int saddll_res_3;
  int saddll_ov_3 = __builtin_saddll_overflow(ll_min, 0LL, &saddll_res_3);

  // -9223372036854775808LL + 0LL = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_res_3 != ll_min);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_ov_3 != 0);


  long long int saddll_res_4;
  int saddll_ov_4 = __builtin_saddll_overflow(ll_max, -1LL, &saddll_res_4);

  // 9223372036854775807LL + -1LL = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_res_4 != 9223372036854775806LL);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_ov_4 != 0);


  long long int saddll_res_5;
  int saddll_ov_5 = __builtin_saddll_overflow(ll_min, 1LL, &saddll_res_5);

  // -9223372036854775808LL + 1LL = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_res_5 != -ll_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_ov_5 != 0);


  long long int saddll_res_6;
  int saddll_ov_6 = __builtin_saddll_overflow(ll_max, 1LL, &saddll_res_6);

  // 9223372036854775807LL + 1LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow =
  // 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_res_6 != ll_min);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_ov_6 != 1);


  long long int saddll_res_7;
  int saddll_ov_7 = __builtin_saddll_overflow(ll_min, -1LL, &saddll_res_7);

  // -9223372036854775808LL + -1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_res_7 != ll_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_ov_7 != 1);


  // Unsigned int addition overflow tests.

  unsigned int uadd_res_1;
  int uadd_ov_1 = __builtin_uadd_overflow(0U, 0U, &uadd_res_1);

  // 0U + 0U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_res_1 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_ov_1 != 0);


  unsigned int uadd_res_2;
  int uadd_ov_2 = __builtin_uadd_overflow(uint_max, 0U, &uadd_res_2);

  // 4294967295U + 0U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_res_2 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_ov_2 != 0);


  unsigned int uadd_res_3;
  int uadd_ov_3 = __builtin_uadd_overflow(4294967294U, 1U, &uadd_res_3);

  // 4294967294U + 1U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_res_3 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_ov_3 != 0);


  unsigned int uadd_res_4;
  int uadd_ov_4 = __builtin_uadd_overflow(uint_max, 1U, &uadd_res_4);

  // 4294967295U + 1U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_res_4 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_ov_4 != 1);


  unsigned int uadd_res_5;
  int uadd_ov_5 = __builtin_uadd_overflow(uint_max, 2U, &uadd_res_5);

  // 4294967295U + 2U = 4294967297, outside the destination range; stored result = 1U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_res_5 != 1U);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_ov_5 != 1);


  unsigned int uadd_res_6;
  int uadd_ov_6 = __builtin_uadd_overflow(uint_max, uint_max, &uadd_res_6);

  // 4294967295U + 4294967295U = 8589934590, outside the destination range; stored result = 4294967294U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_res_6 != 4294967294U);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_ov_6 != 1);


  // Unsigned long int addition overflow tests.

  const unsigned long int uaddl_max = ~0UL;


  unsigned long int uaddl_res_1;
  int uaddl_ov_1 = __builtin_uaddl_overflow(0UL, 0UL, &uaddl_res_1);

  // 0UL + 0UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_res_1 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_ov_1 != 0);


  unsigned long int uaddl_res_2;
  int uaddl_ov_2 = __builtin_uaddl_overflow(uaddl_max, 0UL, &uaddl_res_2);

  // 18446744073709551615UL + 0UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_res_2 != uaddl_max);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_ov_2 != 0);


  unsigned long int uaddl_res_3;
  int uaddl_ov_3 = __builtin_uaddl_overflow((~0UL) - 1UL, 1UL, &uaddl_res_3);

  // 18446744073709551614UL + 1UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_res_3 != uaddl_max);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_ov_3 != 0);


  unsigned long int uaddl_res_4;
  int uaddl_ov_4 = __builtin_uaddl_overflow(uaddl_max, 1UL, &uaddl_res_4);

  // 18446744073709551615UL + 1UL = 18446744073709551616, outside the destination range; stored result = 0UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_res_4 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_ov_4 != 1);


  unsigned long int uaddl_res_5;
  int uaddl_ov_5 = __builtin_uaddl_overflow(uaddl_max, 2UL, &uaddl_res_5);

  // 18446744073709551615UL + 2UL = 18446744073709551617, outside the destination range; stored result = 1UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_res_5 != 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_ov_5 != 1);


  unsigned long int uaddl_res_6;
  int uaddl_ov_6 = __builtin_uaddl_overflow(uaddl_max, uaddl_max, &uaddl_res_6);

  // 18446744073709551615UL + 18446744073709551615UL = 36893488147419103230, outside the destination range; stored result =
  // 18446744073709551614UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_res_6 != (~0UL) - 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_ov_6 != 1);


  // Unsigned long long int addition overflow tests.

  unsigned long long int uaddll_res_1;
  int uaddll_ov_1 = __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_res_1);

  // 0ULL + 0ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_res_1 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_ov_1 != 0);


  unsigned long long int uaddll_res_2;
  int uaddll_ov_2 = __builtin_uaddll_overflow(ull_max, 0ULL, &uaddll_res_2);

  // 18446744073709551615ULL + 0ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_res_2 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_ov_2 != 0);


  unsigned long long int uaddll_res_3;
  int uaddll_ov_3 = __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_res_3);

  // 18446744073709551614ULL + 1ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_res_3 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_ov_3 != 0);


  unsigned long long int uaddll_res_4;
  int uaddll_ov_4 = __builtin_uaddll_overflow(ull_max, 1ULL, &uaddll_res_4);

  // 18446744073709551615ULL + 1ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_res_4 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_ov_4 != 1);


  unsigned long long int uaddll_res_5;
  int uaddll_ov_5 = __builtin_uaddll_overflow(ull_max, 2ULL, &uaddll_res_5);

  // 18446744073709551615ULL + 2ULL = 18446744073709551617, outside the destination range; stored result = 1ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_res_5 != 1ULL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_ov_5 != 1);


  unsigned long long int uaddll_res_6;
  int uaddll_ov_6 = __builtin_uaddll_overflow(ull_max, ull_max, &uaddll_res_6);

  // 18446744073709551615ULL + 18446744073709551615ULL = 36893488147419103230, outside the destination range; stored result =
  // 18446744073709551614ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_res_6 != 18446744073709551614ULL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_ov_6 != 1);


  // If no expected-value check fails, every deliberately negated check is false.
  if (!(all_expected_checks_fail))
    goto ERROR;

  return 0;

ERROR:
  return 1;
}
