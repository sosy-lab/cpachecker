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
  const signed char signed_char_min = -128;
  const signed char signed_char_max = 127;
  const unsigned char unsigned_char_max = 255U;
  const short int short_min = -32768;
  const short int short_max = 32767;
  const int int_min = (-2147483647 - 1);
  const int int_max = 2147483647;
  const unsigned int unsigned_int_max = 4294967295U;
  const long long int long_long_min = (-9223372036854775807LL - 1LL);
  const long long int long_long_max = 9223372036854775807LL;
  const unsigned long long int unsigned_long_long_max = 18446744073709551615ULL;


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: 2147483647L + 1L = 2147483648, within long range; result = 2147483648L and overflow = 0.
  long int model_saddl_result;
  int model_saddl_overflow = __builtin_saddl_overflow(2147483647L, 1L, &model_saddl_result);
  all_expected_checks_fail = all_expected_checks_fail || (model_saddl_result == 0L);
  all_expected_checks_fail = all_expected_checks_fail || (model_saddl_overflow != 0);

  // Generic addition overflow tests.

  const unsigned long int add_overflow_unsigned_long_max = ~0UL;


  const long int add_overflow_long_max = (long int)((~0UL) >> 1);


  const long int add_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);


  signed char add_overflow_result_1;
  int add_overflow_overflow_1 = __builtin_add_overflow(signed_char_max, 0, &add_overflow_result_1);

  // 127 + 0 = 127, which fits the destination range; stored result = 127 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_1 != signed_char_max);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_1 != 0);


  signed char add_overflow_result_2;
  int add_overflow_overflow_2 = __builtin_add_overflow(signed_char_max, 1, &add_overflow_result_2);

  // 127 + 1 = 128, outside the destination range; stored result = -128 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_2 != signed_char_min);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_2 != 1);


  unsigned char add_overflow_result_3;
  int add_overflow_overflow_3 = __builtin_add_overflow(unsigned_char_max, 1, &add_overflow_result_3);

  // 255U + 1 = 256, outside the destination range; stored result = 0 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_3 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_3 != 1);


  short int add_overflow_result_4;
  int add_overflow_overflow_4 = __builtin_add_overflow(short_min, -1, &add_overflow_result_4);

  // -32768 + -1 = -32769, outside the destination range; stored result = 32767 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_4 != short_max);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_4 != 1);


  unsigned short int add_overflow_result_5;
  int add_overflow_overflow_5 = __builtin_add_overflow(-1, 1U, &add_overflow_result_5);

  // -1 + 1U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_5 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_5 != 0);


  int add_overflow_result_6;
  int add_overflow_overflow_6 = __builtin_add_overflow(int_max, 1, &add_overflow_result_6);

  // 2147483647 + 1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_6 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_6 != 1);


  unsigned int add_overflow_result_7;
  int add_overflow_overflow_7 = __builtin_add_overflow(unsigned_int_max, 1U, &add_overflow_result_7);

  // 4294967295U + 1U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_7 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_7 != 1);


  long int add_overflow_result_8;
  int add_overflow_overflow_8 = __builtin_add_overflow(add_overflow_long_max, 1L, &add_overflow_result_8);

  // 9223372036854775807L + 1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_8 != add_overflow_long_min);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_8 != 1);


  unsigned long int add_overflow_result_9;
  int add_overflow_overflow_9 = __builtin_add_overflow(add_overflow_unsigned_long_max, 1UL, &add_overflow_result_9);

  // 18446744073709551615UL + 1UL = 18446744073709551616, outside the destination range; stored result = 0UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_9 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_9 != 1);


  long long int add_overflow_result_10;
  int add_overflow_overflow_10 = __builtin_add_overflow(long_long_min, -1LL, &add_overflow_result_10);

  // -9223372036854775808LL + -1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_10 != long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_10 != 1);


  unsigned long long int add_overflow_result_11;
  int add_overflow_overflow_11 = __builtin_add_overflow(unsigned_long_long_max, 1ULL, &add_overflow_result_11);

  // 18446744073709551615ULL + 1ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_11 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_11 != 1);


  char add_overflow_result_12;
  int add_overflow_overflow_12 = __builtin_add_overflow(char_max, 1, &add_overflow_result_12);

  // (char)127 + 1 = 128, outside the destination range; stored result = (char)-128 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_12 != char_min);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_12 != 1);


  int add_overflow_result_13;
  int add_overflow_overflow_13 = __builtin_add_overflow(-1LL, 1ULL, &add_overflow_result_13);

  // -1LL + 1ULL = 0, which fits the destination range; stored result = 0 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_13 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_13 != 0);


  int add_overflow_result_14;
  int add_overflow_overflow_14 = __builtin_add_overflow(unsigned_long_long_max, 0, &add_overflow_result_14);

  // 18446744073709551615ULL + 0 = 18446744073709551615, outside the destination range; stored result = (int)18446744073709551615ULL and
  // overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_14 != (int)unsigned_long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_14 != 1);


  // Signed int addition overflow tests.

  int sadd_overflow_result_1;
  int sadd_overflow_overflow_1 = __builtin_sadd_overflow(0, 0, &sadd_overflow_result_1);

  // 0 + 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_1 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_1 != 0);


  int sadd_overflow_result_2;
  int sadd_overflow_overflow_2 = __builtin_sadd_overflow(int_max, 0, &sadd_overflow_result_2);

  // 2147483647 + 0 = 2147483647, which fits the destination range; stored result = 2147483647 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_2 != int_max);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_2 != 0);


  int sadd_overflow_result_3;
  int sadd_overflow_overflow_3 = __builtin_sadd_overflow(int_min, 0, &sadd_overflow_result_3);

  // -2147483648 + 0 = -2147483648, which fits the destination range; stored result = -2147483648 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_3 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_3 != 0);


  int sadd_overflow_result_4;
  int sadd_overflow_overflow_4 = __builtin_sadd_overflow(int_max, -1, &sadd_overflow_result_4);

  // 2147483647 + -1 = 2147483646, which fits the destination range; stored result = 2147483646 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_4 != 2147483646);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_4 != 0);


  int sadd_overflow_result_5;
  int sadd_overflow_overflow_5 = __builtin_sadd_overflow(int_min, 1, &sadd_overflow_result_5);

  // -2147483648 + 1 = -2147483647, which fits the destination range; stored result = -2147483647 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_5 != -int_max);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_5 != 0);


  int sadd_overflow_result_6;
  int sadd_overflow_overflow_6 = __builtin_sadd_overflow(int_max, 1, &sadd_overflow_result_6);

  // 2147483647 + 1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_6 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_6 != 1);


  int sadd_overflow_result_7;
  int sadd_overflow_overflow_7 = __builtin_sadd_overflow(int_min, -1, &sadd_overflow_result_7);

  // -2147483648 + -1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_7 != int_max);

  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_7 != 1);


  // Signed long int addition overflow tests.


  const long int saddl_overflow_long_max = (long int)((~0UL) >> 1);


  const long int saddl_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);


  long int saddl_overflow_result_1;
  int saddl_overflow_overflow_1 = __builtin_saddl_overflow(0L, 0L, &saddl_overflow_result_1);

  // 0L + 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_1 != 0L);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_1 != 0);


  long int saddl_overflow_result_2;
  int saddl_overflow_overflow_2 = __builtin_saddl_overflow(saddl_overflow_long_max, 0L, &saddl_overflow_result_2);

  // 9223372036854775807L + 0L = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807L and overflow =
  // 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_2 != saddl_overflow_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_2 != 0);


  long int saddl_overflow_result_3;
  int saddl_overflow_overflow_3 = __builtin_saddl_overflow(saddl_overflow_long_min, 0L, &saddl_overflow_result_3);

  // -9223372036854775808L + 0L = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808L and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_3 != saddl_overflow_long_min);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_3 != 0);


  long int saddl_overflow_result_4;
  int saddl_overflow_overflow_4 = __builtin_saddl_overflow(saddl_overflow_long_max, -1L, &saddl_overflow_result_4);

  // 9223372036854775807L + -1L = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806L and overflow =
  // 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_4 != ((long int)((~0UL) >> 1)) - 1L);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_4 != 0);


  long int saddl_overflow_result_5;
  int saddl_overflow_overflow_5 = __builtin_saddl_overflow(saddl_overflow_long_min, 1L, &saddl_overflow_result_5);

  // saddl_overflow_long_min + 1L = saddl_overflow_long_min + 1L, which fits the destination range; stored result = -9223372036854775807L
  // and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_5 != ((-((long int)((~0UL) >> 1)) - 1L)) + 1L);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_5 != 0);


  long int saddl_overflow_result_6;
  int saddl_overflow_overflow_6 = __builtin_saddl_overflow(saddl_overflow_long_max, 1L, &saddl_overflow_result_6);

  // 9223372036854775807L + 1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_6 != saddl_overflow_long_min);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_6 != 1);


  long int saddl_overflow_result_7;
  int saddl_overflow_overflow_7 = __builtin_saddl_overflow(saddl_overflow_long_min, -1L, &saddl_overflow_result_7);

  // -9223372036854775808L + -1L = -9223372036854775809, outside the destination range; stored result = 9223372036854775807L and overflow =
  // 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_7 != saddl_overflow_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_7 != 1);


  // Signed long long int addition overflow tests.

  long long int saddll_overflow_result_1;
  int saddll_overflow_overflow_1 = __builtin_saddll_overflow(0LL, 0LL, &saddll_overflow_result_1);

  // 0LL + 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_1 != 0LL);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_1 != 0);


  long long int saddll_overflow_result_2;
  int saddll_overflow_overflow_2 = __builtin_saddll_overflow(long_long_max, 0LL, &saddll_overflow_result_2);

  // 9223372036854775807LL + 0LL = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807LL and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_2 != long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_2 != 0);


  long long int saddll_overflow_result_3;
  int saddll_overflow_overflow_3 = __builtin_saddll_overflow(long_long_min, 0LL, &saddll_overflow_result_3);

  // -9223372036854775808LL + 0LL = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_3 != long_long_min);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_3 != 0);


  long long int saddll_overflow_result_4;
  int saddll_overflow_overflow_4 = __builtin_saddll_overflow(long_long_max, -1LL, &saddll_overflow_result_4);

  // 9223372036854775807LL + -1LL = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_4 != 9223372036854775806LL);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_4 != 0);


  long long int saddll_overflow_result_5;
  int saddll_overflow_overflow_5 = __builtin_saddll_overflow(long_long_min, 1LL, &saddll_overflow_result_5);

  // -9223372036854775808LL + 1LL = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_5 != -long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_5 != 0);


  long long int saddll_overflow_result_6;
  int saddll_overflow_overflow_6 = __builtin_saddll_overflow(long_long_max, 1LL, &saddll_overflow_result_6);

  // 9223372036854775807LL + 1LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow =
  // 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_6 != long_long_min);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_6 != 1);


  long long int saddll_overflow_result_7;
  int saddll_overflow_overflow_7 = __builtin_saddll_overflow(long_long_min, -1LL, &saddll_overflow_result_7);

  // -9223372036854775808LL + -1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_7 != long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_7 != 1);


  // Unsigned int addition overflow tests.

  unsigned int uadd_overflow_result_1;
  int uadd_overflow_overflow_1 = __builtin_uadd_overflow(0U, 0U, &uadd_overflow_result_1);

  // 0U + 0U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_1 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_1 != 0);


  unsigned int uadd_overflow_result_2;
  int uadd_overflow_overflow_2 = __builtin_uadd_overflow(unsigned_int_max, 0U, &uadd_overflow_result_2);

  // 4294967295U + 0U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_2 != unsigned_int_max);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_2 != 0);


  unsigned int uadd_overflow_result_3;
  int uadd_overflow_overflow_3 = __builtin_uadd_overflow(4294967294U, 1U, &uadd_overflow_result_3);

  // 4294967294U + 1U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_3 != unsigned_int_max);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_3 != 0);


  unsigned int uadd_overflow_result_4;
  int uadd_overflow_overflow_4 = __builtin_uadd_overflow(unsigned_int_max, 1U, &uadd_overflow_result_4);

  // 4294967295U + 1U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_4 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_4 != 1);


  unsigned int uadd_overflow_result_5;
  int uadd_overflow_overflow_5 = __builtin_uadd_overflow(unsigned_int_max, 2U, &uadd_overflow_result_5);

  // 4294967295U + 2U = 4294967297, outside the destination range; stored result = 1U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_5 != 1U);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_5 != 1);


  unsigned int uadd_overflow_result_6;
  int uadd_overflow_overflow_6 = __builtin_uadd_overflow(unsigned_int_max, unsigned_int_max, &uadd_overflow_result_6);

  // 4294967295U + 4294967295U = 8589934590, outside the destination range; stored result = 4294967294U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_6 != 4294967294U);

  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_6 != 1);


  // Unsigned long int addition overflow tests.

  const unsigned long int uaddl_overflow_unsigned_long_max = ~0UL;


  unsigned long int uaddl_overflow_result_1;
  int uaddl_overflow_overflow_1 = __builtin_uaddl_overflow(0UL, 0UL, &uaddl_overflow_result_1);

  // 0UL + 0UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_1 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_1 != 0);


  unsigned long int uaddl_overflow_result_2;
  int uaddl_overflow_overflow_2 = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 0UL, &uaddl_overflow_result_2);

  // 18446744073709551615UL + 0UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_2 != uaddl_overflow_unsigned_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_2 != 0);


  unsigned long int uaddl_overflow_result_3;
  int uaddl_overflow_overflow_3 = __builtin_uaddl_overflow((~0UL) - 1UL, 1UL, &uaddl_overflow_result_3);

  // 18446744073709551614UL + 1UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_3 != uaddl_overflow_unsigned_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_3 != 0);


  unsigned long int uaddl_overflow_result_4;
  int uaddl_overflow_overflow_4 = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 1UL, &uaddl_overflow_result_4);

  // 18446744073709551615UL + 1UL = 18446744073709551616, outside the destination range; stored result = 0UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_4 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_4 != 1);


  unsigned long int uaddl_overflow_result_5;
  int uaddl_overflow_overflow_5 = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 2UL, &uaddl_overflow_result_5);

  // 18446744073709551615UL + 2UL = 18446744073709551617, outside the destination range; stored result = 1UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_5 != 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_5 != 1);


  unsigned long int uaddl_overflow_result_6;
  int uaddl_overflow_overflow_6 = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, uaddl_overflow_unsigned_long_max, &uaddl_overflow_result_6);

  // 18446744073709551615UL + 18446744073709551615UL = 36893488147419103230, outside the destination range; stored result =
  // 18446744073709551614UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_6 != (~0UL) - 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_6 != 1);


  // Unsigned long long int addition overflow tests.

  unsigned long long int uaddll_overflow_result_1;
  int uaddll_overflow_overflow_1 = __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_overflow_result_1);

  // 0ULL + 0ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_1 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_1 != 0);


  unsigned long long int uaddll_overflow_result_2;
  int uaddll_overflow_overflow_2 = __builtin_uaddll_overflow(unsigned_long_long_max, 0ULL, &uaddll_overflow_result_2);

  // 18446744073709551615ULL + 0ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_2 != unsigned_long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_2 != 0);


  unsigned long long int uaddll_overflow_result_3;
  int uaddll_overflow_overflow_3 = __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_overflow_result_3);

  // 18446744073709551614ULL + 1ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_3 != unsigned_long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_3 != 0);


  unsigned long long int uaddll_overflow_result_4;
  int uaddll_overflow_overflow_4 = __builtin_uaddll_overflow(unsigned_long_long_max, 1ULL, &uaddll_overflow_result_4);

  // 18446744073709551615ULL + 1ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_4 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_4 != 1);


  unsigned long long int uaddll_overflow_result_5;
  int uaddll_overflow_overflow_5 = __builtin_uaddll_overflow(unsigned_long_long_max, 2ULL, &uaddll_overflow_result_5);

  // 18446744073709551615ULL + 2ULL = 18446744073709551617, outside the destination range; stored result = 1ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_5 != 1ULL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_5 != 1);


  unsigned long long int uaddll_overflow_result_6;
  int uaddll_overflow_overflow_6 = __builtin_uaddll_overflow(unsigned_long_long_max, unsigned_long_long_max, &uaddll_overflow_result_6);

  // 18446744073709551615ULL + 18446744073709551615ULL = 36893488147419103230, outside the destination range; stored result =
  // 18446744073709551614ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_6 != 18446744073709551614ULL);

  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_6 != 1);


  // If no expected-value check fails, every deliberately negated check is false.
  if (!(all_expected_checks_fail))
    goto ERROR;

  return 0;

ERROR:
  return 1;
}
