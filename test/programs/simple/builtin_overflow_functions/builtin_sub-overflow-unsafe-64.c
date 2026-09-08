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
  const unsigned short int ushort_max = 65535U;
  const int int_min = (-2147483647 - 1);
  const int int_max = 2147483647;
  const unsigned int uint_max = 4294967295U;
  const long long int ll_min = (-9223372036854775807LL - 1LL);
  const long long int ll_max = 9223372036854775807LL;
  const unsigned long long int ull_max = 18446744073709551615ULL;


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: -2147483648L - 1L fits in long, stores -2147483649L, and returns 0.
  const long long int model_ssubl_expected_res = -2147483649LL;
  long int model_ssubl_res;
  int model_ssubl_ov = __builtin_ssubl_overflow((-2147483647L - 1L), 1L, &model_ssubl_res);
  all_expected_checks_fail =
    all_expected_checks_fail || ((long long int)model_ssubl_res != model_ssubl_expected_res);
  all_expected_checks_fail = all_expected_checks_fail || (model_ssubl_ov != 0);

  // Generic subtraction overflow tests.

  const unsigned long int sub_ulong_max = ~0UL;


  const long int sub_long_max = (long int)((~0UL) >> 1);


  const long int sub_long_min = (-((long int)((~0UL) >> 1)) - 1L);


  signed char sub_res_1;
  int sub_ov_1 = __builtin_sub_overflow(schar_min, 1, &sub_res_1);

  // -128 - 1 = -129, outside the destination range; stored result = 127 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_1 != schar_max);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_1 != 1);


  unsigned char sub_res_2;
  int sub_ov_2 = __builtin_sub_overflow(0, 1, &sub_res_2);

  // 0 - 1 = -1, outside the destination range; stored result = 255U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_2 != uchar_max);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_2 != 1);


  short int sub_res_3;
  int sub_ov_3 = __builtin_sub_overflow(short_max, -1, &sub_res_3);

  // 32767 - -1 = 32768, outside the destination range; stored result = -32768 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_3 != short_min);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_3 != 1);


  unsigned short int sub_res_4;
  int sub_ov_4 = __builtin_sub_overflow(ushort_max, ushort_max, &sub_res_4);

  // 65535U - 65535U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_4 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_4 != 0);


  int sub_res_5;
  int sub_ov_5 = __builtin_sub_overflow(int_min, 1, &sub_res_5);

  // -2147483648 - 1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_5 != int_max);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_5 != 1);


  unsigned int sub_res_6;
  int sub_ov_6 = __builtin_sub_overflow(0U, uint_max, &sub_res_6);

  // 0U - 4294967295U = -4294967295, outside the destination range; stored result = 1U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_6 != 1U);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_6 != 1);


  long int sub_res_7;
  int sub_ov_7 = __builtin_sub_overflow(sub_long_max, -1L, &sub_res_7);

  // 9223372036854775807L - -1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_7 != sub_long_min);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_7 != 1);


  unsigned long int sub_res_8;
  int sub_ov_8 = __builtin_sub_overflow(0UL, 1UL, &sub_res_8);

  // 0UL - 1UL = -1, outside the destination range; stored result = 18446744073709551615UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_8 != sub_ulong_max);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_8 != 1);


  long long int sub_res_9;
  int sub_ov_9 = __builtin_sub_overflow(0LL, ll_min, &sub_res_9);

  // 0LL - -9223372036854775808LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_9 != ll_min);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_9 != 1);


  unsigned long long int sub_res_10;
  int sub_ov_10 = __builtin_sub_overflow(ull_max, ull_max, &sub_res_10);

  // 18446744073709551615ULL - 18446744073709551615ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_10 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_10 != 0);


  char sub_res_11;
  int sub_ov_11 = __builtin_sub_overflow(char_min, 1, &sub_res_11);

  // (char)-128 - 1 = -129, outside the destination range; stored result = (char)127 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_11 != char_max);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_11 != 1);


  int sub_res_12;
  int sub_ov_12 = __builtin_sub_overflow(1ULL, 1LL, &sub_res_12);

  // 1ULL - 1LL = 0, which fits the destination range; stored result = 0 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_12 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_12 != 0);


  unsigned int sub_res_13;
  int sub_ov_13 = __builtin_sub_overflow(-1LL, 0ULL, &sub_res_13);

  // -1LL - 0ULL = -1, outside the destination range; stored result = 4294967295U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_res_13 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (sub_ov_13 != 1);


  // Signed int subtraction overflow tests.

  int ssub_res_1;
  int ssub_ov_1 = __builtin_ssub_overflow(0, 0, &ssub_res_1);

  // 0 - 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_res_1 != 0);

  all_expected_checks_fail = all_expected_checks_fail || (ssub_ov_1 != 0);


  int ssub_res_2;
  int ssub_ov_2 = __builtin_ssub_overflow(int_max, 0, &ssub_res_2);

  // 2147483647 - 0 = 2147483647, which fits the destination range; stored result = 2147483647 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_res_2 != int_max);

  all_expected_checks_fail = all_expected_checks_fail || (ssub_ov_2 != 0);


  int ssub_res_3;
  int ssub_ov_3 = __builtin_ssub_overflow(int_min, 0, &ssub_res_3);

  // -2147483648 - 0 = -2147483648, which fits the destination range; stored result = -2147483648 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_res_3 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (ssub_ov_3 != 0);


  int ssub_res_4;
  int ssub_ov_4 = __builtin_ssub_overflow(int_min, -1, &ssub_res_4);

  // -2147483648 - -1 = -2147483647, which fits the destination range; stored result = -2147483647 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_res_4 != -int_max);

  all_expected_checks_fail = all_expected_checks_fail || (ssub_ov_4 != 0);


  int ssub_res_5;
  int ssub_ov_5 = __builtin_ssub_overflow(int_max, 1, &ssub_res_5);

  // 2147483647 - 1 = 2147483646, which fits the destination range; stored result = 2147483646 and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_res_5 != 2147483646);

  all_expected_checks_fail = all_expected_checks_fail || (ssub_ov_5 != 0);


  int ssub_res_6;
  int ssub_ov_6 = __builtin_ssub_overflow(int_max, -1, &ssub_res_6);

  // 2147483647 - -1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_res_6 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (ssub_ov_6 != 1);


  int ssub_res_7;
  int ssub_ov_7 = __builtin_ssub_overflow(int_min, 1, &ssub_res_7);

  // -2147483648 - 1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_res_7 != int_max);

  all_expected_checks_fail = all_expected_checks_fail || (ssub_ov_7 != 1);


  int ssub_res_8;
  int ssub_ov_8 = __builtin_ssub_overflow(0, int_min, &ssub_res_8);

  // 0 - -2147483648 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_res_8 != int_min);

  all_expected_checks_fail = all_expected_checks_fail || (ssub_ov_8 != 1);


  // Signed long int subtraction overflow tests.


  const long int ssubl_max = (long int)((~0UL) >> 1);


  const long int ssubl_min = (-((long int)((~0UL) >> 1)) - 1L);


  long int ssubl_res_1;
  int ssubl_ov_1 = __builtin_ssubl_overflow(0L, 0L, &ssubl_res_1);

  // 0L - 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_res_1 != 0L);

  all_expected_checks_fail = all_expected_checks_fail || (ssubl_ov_1 != 0);


  long int ssubl_res_2;
  int ssubl_ov_2 = __builtin_ssubl_overflow(ssubl_max, 0L, &ssubl_res_2);

  // 9223372036854775807L - 0L = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807L and overflow =
  // 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_res_2 != ssubl_max);

  all_expected_checks_fail = all_expected_checks_fail || (ssubl_ov_2 != 0);


  long int ssubl_res_3;
  int ssubl_ov_3 = __builtin_ssubl_overflow(ssubl_min, 0L, &ssubl_res_3);

  // -9223372036854775808L - 0L = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808L and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_res_3 != ssubl_min);

  all_expected_checks_fail = all_expected_checks_fail || (ssubl_ov_3 != 0);


  long int ssubl_res_4;
  int ssubl_ov_4 = __builtin_ssubl_overflow(ssubl_min, -1L, &ssubl_res_4);

  // -9223372036854775808L - -1L = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807L and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_res_4 != ((-((long int)((~0UL) >> 1)) - 1L)) + 1L);

  all_expected_checks_fail = all_expected_checks_fail || (ssubl_ov_4 != 0);


  long int ssubl_res_5;
  int ssubl_ov_5 = __builtin_ssubl_overflow(ssubl_max, 1L, &ssubl_res_5);

  // ssubl_max - 1L = ssubl_max - 1L, which fits the destination range; stored result = 9223372036854775806L and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_res_5 != ((long int)((~0UL) >> 1)) - 1L);

  all_expected_checks_fail = all_expected_checks_fail || (ssubl_ov_5 != 0);


  long int ssubl_res_6;
  int ssubl_ov_6 = __builtin_ssubl_overflow(ssubl_max, -1L, &ssubl_res_6);

  // 9223372036854775807L - -1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_res_6 != ssubl_min);

  all_expected_checks_fail = all_expected_checks_fail || (ssubl_ov_6 != 1);


  long int ssubl_res_7;
  int ssubl_ov_7 = __builtin_ssubl_overflow(ssubl_min, 1L, &ssubl_res_7);

  // -9223372036854775808L - 1L = -9223372036854775809, outside the destination range; stored result = 9223372036854775807L and overflow =
  // 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_res_7 != ssubl_max);

  all_expected_checks_fail = all_expected_checks_fail || (ssubl_ov_7 != 1);


  long int ssubl_res_8;
  int ssubl_ov_8 = __builtin_ssubl_overflow(0L, ssubl_min, &ssubl_res_8);

  // 0L - -9223372036854775808L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_res_8 != ssubl_min);

  all_expected_checks_fail = all_expected_checks_fail || (ssubl_ov_8 != 1);


  // Signed long long int subtraction overflow tests.

  long long int ssubll_res_1;
  int ssubll_ov_1 = __builtin_ssubll_overflow(0LL, 0LL, &ssubll_res_1);

  // 0LL - 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_res_1 != 0LL);

  all_expected_checks_fail = all_expected_checks_fail || (ssubll_ov_1 != 0);


  long long int ssubll_res_2;
  int ssubll_ov_2 = __builtin_ssubll_overflow(ll_max, 0LL, &ssubll_res_2);

  // 9223372036854775807LL - 0LL = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807LL and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_res_2 != ll_max);

  all_expected_checks_fail = all_expected_checks_fail || (ssubll_ov_2 != 0);


  long long int ssubll_res_3;
  int ssubll_ov_3 = __builtin_ssubll_overflow(ll_min, 0LL, &ssubll_res_3);

  // -9223372036854775808LL - 0LL = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_res_3 != ll_min);

  all_expected_checks_fail = all_expected_checks_fail || (ssubll_ov_3 != 0);


  long long int ssubll_res_4;
  int ssubll_ov_4 = __builtin_ssubll_overflow(ll_min, -1LL, &ssubll_res_4);

  // -9223372036854775808LL - -1LL = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807LL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_res_4 != -ll_max);

  all_expected_checks_fail = all_expected_checks_fail || (ssubll_ov_4 != 0);


  long long int ssubll_res_5;
  int ssubll_ov_5 = __builtin_ssubll_overflow(ll_max, 1LL, &ssubll_res_5);

  // 9223372036854775807LL - 1LL = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806LL and overflow
  // = 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_res_5 != 9223372036854775806LL);

  all_expected_checks_fail = all_expected_checks_fail || (ssubll_ov_5 != 0);


  long long int ssubll_res_6;
  int ssubll_ov_6 = __builtin_ssubll_overflow(ll_max, -1LL, &ssubll_res_6);

  // 9223372036854775807LL - -1LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_res_6 != ll_min);

  all_expected_checks_fail = all_expected_checks_fail || (ssubll_ov_6 != 1);


  long long int ssubll_res_7;
  int ssubll_ov_7 = __builtin_ssubll_overflow(ll_min, 1LL, &ssubll_res_7);

  // -9223372036854775808LL - 1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_res_7 != ll_max);

  all_expected_checks_fail = all_expected_checks_fail || (ssubll_ov_7 != 1);


  long long int ssubll_res_8;
  int ssubll_ov_8 = __builtin_ssubll_overflow(0LL, ll_min, &ssubll_res_8);

  // 0LL - -9223372036854775808LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_res_8 != ll_min);

  all_expected_checks_fail = all_expected_checks_fail || (ssubll_ov_8 != 1);


  // Unsigned int subtraction overflow tests.

  unsigned int usub_res_1;
  int usub_ov_1 = __builtin_usub_overflow(0U, 0U, &usub_res_1);

  // 0U - 0U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usub_res_1 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (usub_ov_1 != 0);


  unsigned int usub_res_2;
  int usub_ov_2 = __builtin_usub_overflow(uint_max, 0U, &usub_res_2);

  // 4294967295U - 0U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usub_res_2 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (usub_ov_2 != 0);


  unsigned int usub_res_3;
  int usub_ov_3 = __builtin_usub_overflow(uint_max, uint_max, &usub_res_3);

  // 4294967295U - 4294967295U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usub_res_3 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (usub_ov_3 != 0);


  unsigned int usub_res_4;
  int usub_ov_4 = __builtin_usub_overflow(1U, 1U, &usub_res_4);

  // 1U - 1U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usub_res_4 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (usub_ov_4 != 0);


  unsigned int usub_res_5;
  int usub_ov_5 = __builtin_usub_overflow(0U, 1U, &usub_res_5);

  // 0U - 1U = -1, outside the destination range; stored result = 4294967295U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (usub_res_5 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (usub_ov_5 != 1);


  unsigned int usub_res_6;
  int usub_ov_6 = __builtin_usub_overflow(0U, uint_max, &usub_res_6);

  // 0U - 4294967295U = -4294967295, outside the destination range; stored result = 1U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (usub_res_6 != 1U);

  all_expected_checks_fail = all_expected_checks_fail || (usub_ov_6 != 1);


  unsigned int usub_res_7;
  int usub_ov_7 = __builtin_usub_overflow(1U, 2U, &usub_res_7);

  // 1U - 2U = -1, outside the destination range; stored result = 4294967295U and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (usub_res_7 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (usub_ov_7 != 1);


  // Unsigned long int subtraction overflow tests.

  const unsigned long int usubl_max = ~0UL;


  unsigned long int usubl_res_1;
  int usubl_ov_1 = __builtin_usubl_overflow(0UL, 0UL, &usubl_res_1);

  // 0UL - 0UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_res_1 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (usubl_ov_1 != 0);


  unsigned long int usubl_res_2;
  int usubl_ov_2 = __builtin_usubl_overflow(usubl_max, 0UL, &usubl_res_2);

  // 18446744073709551615UL - 0UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_res_2 != usubl_max);

  all_expected_checks_fail = all_expected_checks_fail || (usubl_ov_2 != 0);


  unsigned long int usubl_res_3;
  int usubl_ov_3 = __builtin_usubl_overflow(usubl_max, usubl_max, &usubl_res_3);

  // 18446744073709551615UL - 18446744073709551615UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_res_3 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (usubl_ov_3 != 0);


  unsigned long int usubl_res_4;
  int usubl_ov_4 = __builtin_usubl_overflow(1UL, 1UL, &usubl_res_4);

  // 1UL - 1UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_res_4 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (usubl_ov_4 != 0);


  unsigned long int usubl_res_5;
  int usubl_ov_5 = __builtin_usubl_overflow(0UL, 1UL, &usubl_res_5);

  // 0UL - 1UL = -1, outside the destination range; stored result = 18446744073709551615UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_res_5 != usubl_max);

  all_expected_checks_fail = all_expected_checks_fail || (usubl_ov_5 != 1);


  unsigned long int usubl_res_6;
  int usubl_ov_6 = __builtin_usubl_overflow(0UL, usubl_max, &usubl_res_6);

  // 0UL - 18446744073709551615UL = -18446744073709551615, outside the destination range; stored result = 1UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_res_6 != 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (usubl_ov_6 != 1);


  unsigned long int usubl_res_7;
  int usubl_ov_7 = __builtin_usubl_overflow(1UL, 2UL, &usubl_res_7);

  // 1UL - 2UL = -1, outside the destination range; stored result = 18446744073709551615UL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_res_7 != usubl_max);

  all_expected_checks_fail = all_expected_checks_fail || (usubl_ov_7 != 1);


  // Unsigned long long int subtraction overflow tests.

  unsigned long long int usubll_res_1;
  int usubll_ov_1 = __builtin_usubll_overflow(0ULL, 0ULL, &usubll_res_1);

  // 0ULL - 0ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_res_1 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (usubll_ov_1 != 0);


  unsigned long long int usubll_res_2;
  int usubll_ov_2 = __builtin_usubll_overflow(ull_max, 0ULL, &usubll_res_2);

  // 18446744073709551615ULL - 0ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_res_2 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (usubll_ov_2 != 0);


  unsigned long long int usubll_res_3;
  int usubll_ov_3 = __builtin_usubll_overflow(ull_max, ull_max, &usubll_res_3);

  // 18446744073709551615ULL - 18446744073709551615ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_res_3 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (usubll_ov_3 != 0);


  unsigned long long int usubll_res_4;
  int usubll_ov_4 = __builtin_usubll_overflow(1ULL, 1ULL, &usubll_res_4);

  // 1ULL - 1ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_res_4 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (usubll_ov_4 != 0);


  unsigned long long int usubll_res_5;
  int usubll_ov_5 = __builtin_usubll_overflow(0ULL, 1ULL, &usubll_res_5);

  // 0ULL - 1ULL = -1, outside the destination range; stored result = 18446744073709551615ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_res_5 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (usubll_ov_5 != 1);


  unsigned long long int usubll_res_6;
  int usubll_ov_6 = __builtin_usubll_overflow(0ULL, ull_max, &usubll_res_6);

  // 0ULL - 18446744073709551615ULL = -18446744073709551615, outside the destination range; stored result = 1ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_res_6 != 1ULL);

  all_expected_checks_fail = all_expected_checks_fail || (usubll_ov_6 != 1);


  unsigned long long int usubll_res_7;
  int usubll_ov_7 = __builtin_usubll_overflow(1ULL, 2ULL, &usubll_res_7);

  // 1ULL - 2ULL = -1, outside the destination range; stored result = 18446744073709551615ULL and overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_res_7 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (usubll_ov_7 != 1);


  // If no expected-value check fails, every deliberately negated check is false.
  if (!(all_expected_checks_fail))
    goto ERROR;

  return 0;

ERROR:
  return 1;
}
