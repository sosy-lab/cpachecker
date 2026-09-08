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
  const unsigned short int unsigned_short_max = 65535U;
  const int int_min = (-2147483647 - 1);
  const int int_max = 2147483647;
  const unsigned int unsigned_int_max = 4294967295U;
  const long long int long_long_min = (-9223372036854775807LL - 1LL);
  const long long int long_long_max = 9223372036854775807LL;
  const unsigned long long int unsigned_long_long_max = 18446744073709551615ULL;


  // This program targets ILP32 and fails its expected verdict under LP64.

  // ILP32: -2147483648L - 1L overflows long, stores 2147483647L, and returns 1.
  long int model_ssubl_result;
  int model_ssubl_overflow = __builtin_ssubl_overflow((-2147483647L - 1L), 1L, &model_ssubl_result);
  all_expected_checks_fail = all_expected_checks_fail || (model_ssubl_result == 0L);
  all_expected_checks_fail = all_expected_checks_fail || (model_ssubl_overflow != 1);

  // Tests for __builtin_sub_overflow.

  const unsigned long int sub_overflow_unsigned_long_max = ~0UL;


  const long int sub_overflow_long_max = (long int)((~0UL) >> 1);


  const long int sub_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);


  signed char sub_overflow_result_1;
  int sub_overflow_overflow_1 = __builtin_sub_overflow(signed_char_min, 1, &sub_overflow_result_1);

  // __builtin_sub_overflow(-128, 1, &sub_overflow_result_1) stores 127.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_1 != signed_char_max);

  // __builtin_sub_overflow(-128, 1, &sub_overflow_result_1) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_1 != 1);


  unsigned char sub_overflow_result_2;
  int sub_overflow_overflow_2 = __builtin_sub_overflow(0, 1, &sub_overflow_result_2);

  // __builtin_sub_overflow(0, 1, &sub_overflow_result_2) stores 255U.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_2 != unsigned_char_max);

  // __builtin_sub_overflow(0, 1, &sub_overflow_result_2) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_2 != 1);


  short int sub_overflow_result_3;
  int sub_overflow_overflow_3 = __builtin_sub_overflow(short_max, -1, &sub_overflow_result_3);

  // __builtin_sub_overflow(32767, -1, &sub_overflow_result_3) stores -32768.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_3 != short_min);

  // __builtin_sub_overflow(32767, -1, &sub_overflow_result_3) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_3 != 1);


  unsigned short int sub_overflow_result_4;
  int sub_overflow_overflow_4 = __builtin_sub_overflow(unsigned_short_max, unsigned_short_max, &sub_overflow_result_4);

  // __builtin_sub_overflow(65535U, 65535U, &sub_overflow_result_4) stores 0U.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_4 != 0U);

  // __builtin_sub_overflow(65535U, 65535U, &sub_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_4 != 0);


  int sub_overflow_result_5;
  int sub_overflow_overflow_5 = __builtin_sub_overflow(int_min, 1, &sub_overflow_result_5);

  // __builtin_sub_overflow((-2147483647 - 1), 1, &sub_overflow_result_5) stores 2147483647.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_5 != int_max);

  // __builtin_sub_overflow((-2147483647 - 1), 1, &sub_overflow_result_5) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_5 != 1);


  unsigned int sub_overflow_result_6;
  int sub_overflow_overflow_6 = __builtin_sub_overflow(0U, unsigned_int_max, &sub_overflow_result_6);

  // __builtin_sub_overflow(0U, 4294967295U, &sub_overflow_result_6) stores 1U.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_6 != 1U);

  // __builtin_sub_overflow(0U, 4294967295U, &sub_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_6 != 1);


  long int sub_overflow_result_7;
  int sub_overflow_overflow_7 = __builtin_sub_overflow(sub_overflow_long_max, -1L, &sub_overflow_result_7);

  // ILP32: __builtin_sub_overflow(2147483647L, -1L, &sub_overflow_result_7) stores -2147483648L.
  // LP64: __builtin_sub_overflow(9223372036854775807L, -1L, &sub_overflow_result_7) stores -9223372036854775808L.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_7 != sub_overflow_long_min);

  // ILP32: __builtin_sub_overflow(2147483647L, -1L, &sub_overflow_result_7) returns 1.
  // LP64: __builtin_sub_overflow(9223372036854775807L, -1L, &sub_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_7 != 1);


  unsigned long int sub_overflow_result_8;
  int sub_overflow_overflow_8 = __builtin_sub_overflow(0UL, 1UL, &sub_overflow_result_8);

  // ILP32: __builtin_sub_overflow(0UL, 1UL, &sub_overflow_result_8) stores 4294967295UL.
  // LP64: __builtin_sub_overflow(0UL, 1UL, &sub_overflow_result_8) stores 18446744073709551615UL.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_8 != sub_overflow_unsigned_long_max);

  // __builtin_sub_overflow(0UL, 1UL, &sub_overflow_result_8) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_8 != 1);


  long long int sub_overflow_result_9;
  int sub_overflow_overflow_9 = __builtin_sub_overflow(0LL, long_long_min, &sub_overflow_result_9);

  // __builtin_sub_overflow(0LL, -9223372036854775808LL, &sub_overflow_result_9) stores -9223372036854775808LL.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_9 != long_long_min);

  // __builtin_sub_overflow(0LL, -9223372036854775808LL, &sub_overflow_result_9) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_9 != 1);


  unsigned long long int sub_overflow_result_10;
  int sub_overflow_overflow_10 = __builtin_sub_overflow(unsigned_long_long_max, unsigned_long_long_max, &sub_overflow_result_10);

  // __builtin_sub_overflow(18446744073709551615ULL, 18446744073709551615ULL, &sub_overflow_result_10) stores 0ULL.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_10 != 0ULL);

  // __builtin_sub_overflow(18446744073709551615ULL, 18446744073709551615ULL, &sub_overflow_result_10) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_10 != 0);


  char sub_overflow_result_11;
  int sub_overflow_overflow_11 = __builtin_sub_overflow(char_min, 1, &sub_overflow_result_11);

  // __builtin_sub_overflow((char)-128, 1, &sub_overflow_result_11) stores (char)127.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_11 != char_max);

  // __builtin_sub_overflow(-128, 1, &sub_overflow_result_11) or __builtin_sub_overflow(0, 1, &sub_overflow_result_11)
  // returns 1, according to plain-char signedness.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_11 != 1);


  int sub_overflow_result_12;
  int sub_overflow_overflow_12 = __builtin_sub_overflow(1ULL, 1LL, &sub_overflow_result_12);

  // __builtin_sub_overflow(1ULL, 1LL, &sub_overflow_result_12) stores 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_12 != 0);

  // __builtin_sub_overflow(1ULL, 1LL, &sub_overflow_result_12) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_12 != 0);


  unsigned int sub_overflow_result_13;
  int sub_overflow_overflow_13 = __builtin_sub_overflow(-1LL, 0ULL, &sub_overflow_result_13);

  // __builtin_sub_overflow(-1LL, 0ULL, &sub_overflow_result_13) stores 4294967295U.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_result_13 != unsigned_int_max);

  // __builtin_sub_overflow(-1LL, 0ULL, &sub_overflow_result_13) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_overflow_13 != 1);


  // Tests for __builtin_ssub_overflow.

  int ssub_overflow_result_1;
  int ssub_overflow_overflow_1 = __builtin_ssub_overflow(0, 0, &ssub_overflow_result_1);

  // __builtin_ssub_overflow(0, 0, &ssub_overflow_result_1) stores 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_result_1 != 0);

  // __builtin_ssub_overflow(0, 0, &ssub_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_overflow_1 != 0);


  int ssub_overflow_result_2;
  int ssub_overflow_overflow_2 = __builtin_ssub_overflow(int_max, 0, &ssub_overflow_result_2);

  // __builtin_ssub_overflow(2147483647, 0, &ssub_overflow_result_2) stores 2147483647.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_result_2 != int_max);

  // __builtin_ssub_overflow(2147483647, 0, &ssub_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_overflow_2 != 0);


  int ssub_overflow_result_3;
  int ssub_overflow_overflow_3 = __builtin_ssub_overflow(int_min, 0, &ssub_overflow_result_3);

  // __builtin_ssub_overflow((-2147483647 - 1), 0, &ssub_overflow_result_3) stores (-2147483647 - 1).
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_result_3 != int_min);

  // __builtin_ssub_overflow((-2147483647 - 1), 0, &ssub_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_overflow_3 != 0);


  int ssub_overflow_result_4;
  int ssub_overflow_overflow_4 = __builtin_ssub_overflow(int_min, -1, &ssub_overflow_result_4);

  // __builtin_ssub_overflow((-2147483647 - 1), -1, &ssub_overflow_result_4) stores (-2147483647 - 1) + 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_result_4 != -int_max);

  // __builtin_ssub_overflow((-2147483647 - 1), -1, &ssub_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_overflow_4 != 0);


  int ssub_overflow_result_5;
  int ssub_overflow_overflow_5 = __builtin_ssub_overflow(int_max, 1, &ssub_overflow_result_5);

  // __builtin_ssub_overflow(2147483647, 1, &ssub_overflow_result_5) stores 2147483647 - 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_result_5 != 2147483646);

  // __builtin_ssub_overflow(2147483647, 1, &ssub_overflow_result_5) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_overflow_5 != 0);


  int ssub_overflow_result_6;
  int ssub_overflow_overflow_6 = __builtin_ssub_overflow(int_max, -1, &ssub_overflow_result_6);

  // __builtin_ssub_overflow(2147483647, -1, &ssub_overflow_result_6) stores (-2147483647 - 1).
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_result_6 != int_min);

  // __builtin_ssub_overflow(2147483647, -1, &ssub_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_overflow_6 != 1);


  int ssub_overflow_result_7;
  int ssub_overflow_overflow_7 = __builtin_ssub_overflow(int_min, 1, &ssub_overflow_result_7);

  // __builtin_ssub_overflow((-2147483647 - 1), 1, &ssub_overflow_result_7) stores 2147483647.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_result_7 != int_max);

  // __builtin_ssub_overflow((-2147483647 - 1), 1, &ssub_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_overflow_7 != 1);


  int ssub_overflow_result_8;
  int ssub_overflow_overflow_8 = __builtin_ssub_overflow(0, int_min, &ssub_overflow_result_8);

  // __builtin_ssub_overflow(0, (-2147483647 - 1), &ssub_overflow_result_8) stores (-2147483647 - 1).
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_result_8 != int_min);

  // __builtin_ssub_overflow(0, (-2147483647 - 1), &ssub_overflow_result_8) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssub_overflow_overflow_8 != 1);


  // Tests for __builtin_ssubl_overflow.


  const long int ssubl_overflow_long_max = (long int)((~0UL) >> 1);


  const long int ssubl_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);


  long int ssubl_overflow_result_1;
  int ssubl_overflow_overflow_1 = __builtin_ssubl_overflow(0L, 0L, &ssubl_overflow_result_1);

  // __builtin_ssubl_overflow(0L, 0L, &ssubl_overflow_result_1) stores 0L.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_result_1 != 0L);

  // __builtin_ssubl_overflow(0L, 0L, &ssubl_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_overflow_1 != 0);


  long int ssubl_overflow_result_2;
  int ssubl_overflow_overflow_2 = __builtin_ssubl_overflow(ssubl_overflow_long_max, 0L, &ssubl_overflow_result_2);

  // ILP32: __builtin_ssubl_overflow(2147483647L, 0L, &ssubl_overflow_result_2) stores 2147483647L.
  // LP64: __builtin_ssubl_overflow(9223372036854775807L, 0L, &ssubl_overflow_result_2) stores 9223372036854775807L.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_result_2 != ssubl_overflow_long_max);

  // ILP32: __builtin_ssubl_overflow(2147483647L, 0L, &ssubl_overflow_result_2) returns 0.
  // LP64: __builtin_ssubl_overflow(9223372036854775807L, 0L, &ssubl_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_overflow_2 != 0);


  long int ssubl_overflow_result_3;
  int ssubl_overflow_overflow_3 = __builtin_ssubl_overflow(ssubl_overflow_long_min, 0L, &ssubl_overflow_result_3);

  // ILP32: __builtin_ssubl_overflow(-2147483648L, 0L, &ssubl_overflow_result_3) stores -2147483648L.
  // LP64: __builtin_ssubl_overflow(-9223372036854775808L, 0L, &ssubl_overflow_result_3) stores -9223372036854775808L.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_result_3 != ssubl_overflow_long_min);

  // ILP32: __builtin_ssubl_overflow(-2147483648L, 0L, &ssubl_overflow_result_3) returns 0.
  // LP64: __builtin_ssubl_overflow(-9223372036854775808L, 0L, &ssubl_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_overflow_3 != 0);


  long int ssubl_overflow_result_4;
  int ssubl_overflow_overflow_4 = __builtin_ssubl_overflow(ssubl_overflow_long_min, -1L, &ssubl_overflow_result_4);

  // ILP32: __builtin_ssubl_overflow(-2147483648L, -1L, &ssubl_overflow_result_4) stores -2147483648L + 1L.
  // LP64: __builtin_ssubl_overflow(-9223372036854775808L, -1L, &ssubl_overflow_result_4) stores -9223372036854775808L + 1L.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_result_4 != ((-((long int)((~0UL) >> 1)) - 1L)) + 1L);

  // ILP32: __builtin_ssubl_overflow(-2147483648L, -1L, &ssubl_overflow_result_4) returns 0.
  // LP64: __builtin_ssubl_overflow(-9223372036854775808L, -1L, &ssubl_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_overflow_4 != 0);


  long int ssubl_overflow_result_5;
  int ssubl_overflow_overflow_5 = __builtin_ssubl_overflow(ssubl_overflow_long_max, 1L, &ssubl_overflow_result_5);

  // ILP32: __builtin_ssubl_overflow(2147483647L, 1L, &ssubl_overflow_result_5) stores 2147483647L - 1L.
  // LP64: __builtin_ssubl_overflow(9223372036854775807L, 1L, &ssubl_overflow_result_5) stores 9223372036854775807L - 1L.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_result_5 != ((long int)((~0UL) >> 1)) - 1L);

  // ILP32: __builtin_ssubl_overflow(2147483647L, 1L, &ssubl_overflow_result_5) returns 0.
  // LP64: __builtin_ssubl_overflow(9223372036854775807L, 1L, &ssubl_overflow_result_5) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_overflow_5 != 0);


  long int ssubl_overflow_result_6;
  int ssubl_overflow_overflow_6 = __builtin_ssubl_overflow(ssubl_overflow_long_max, -1L, &ssubl_overflow_result_6);

  // ILP32: __builtin_ssubl_overflow(2147483647L, -1L, &ssubl_overflow_result_6) stores -2147483648L.
  // LP64: __builtin_ssubl_overflow(9223372036854775807L, -1L, &ssubl_overflow_result_6) stores -9223372036854775808L.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_result_6 != ssubl_overflow_long_min);

  // ILP32: __builtin_ssubl_overflow(2147483647L, -1L, &ssubl_overflow_result_6) returns 1.
  // LP64: __builtin_ssubl_overflow(9223372036854775807L, -1L, &ssubl_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_overflow_6 != 1);


  long int ssubl_overflow_result_7;
  int ssubl_overflow_overflow_7 = __builtin_ssubl_overflow(ssubl_overflow_long_min, 1L, &ssubl_overflow_result_7);

  // ILP32: __builtin_ssubl_overflow(-2147483648L, 1L, &ssubl_overflow_result_7) stores 2147483647L.
  // LP64: __builtin_ssubl_overflow(-9223372036854775808L, 1L, &ssubl_overflow_result_7) stores 9223372036854775807L.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_result_7 != ssubl_overflow_long_max);

  // ILP32: __builtin_ssubl_overflow(-2147483648L, 1L, &ssubl_overflow_result_7) returns 1.
  // LP64: __builtin_ssubl_overflow(-9223372036854775808L, 1L, &ssubl_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_overflow_7 != 1);


  long int ssubl_overflow_result_8;
  int ssubl_overflow_overflow_8 = __builtin_ssubl_overflow(0L, ssubl_overflow_long_min, &ssubl_overflow_result_8);

  // ILP32: __builtin_ssubl_overflow(0L, -2147483648L, &ssubl_overflow_result_8) stores -2147483648L.
  // LP64: __builtin_ssubl_overflow(0L, -9223372036854775808L, &ssubl_overflow_result_8) stores -9223372036854775808L.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_result_8 != ssubl_overflow_long_min);

  // ILP32: __builtin_ssubl_overflow(0L, -2147483648L, &ssubl_overflow_result_8) returns 1.
  // LP64: __builtin_ssubl_overflow(0L, -9223372036854775808L, &ssubl_overflow_result_8) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubl_overflow_overflow_8 != 1);


  // Tests for __builtin_ssubll_overflow.

  long long int ssubll_overflow_result_1;
  int ssubll_overflow_overflow_1 = __builtin_ssubll_overflow(0LL, 0LL, &ssubll_overflow_result_1);

  // __builtin_ssubll_overflow(0LL, 0LL, &ssubll_overflow_result_1) stores 0LL.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_result_1 != 0LL);

  // __builtin_ssubll_overflow(0LL, 0LL, &ssubll_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_overflow_1 != 0);


  long long int ssubll_overflow_result_2;
  int ssubll_overflow_overflow_2 = __builtin_ssubll_overflow(long_long_max, 0LL, &ssubll_overflow_result_2);

  // __builtin_ssubll_overflow(9223372036854775807LL, 0LL, &ssubll_overflow_result_2) stores 9223372036854775807LL.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_result_2 != long_long_max);

  // __builtin_ssubll_overflow(9223372036854775807LL, 0LL, &ssubll_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_overflow_2 != 0);


  long long int ssubll_overflow_result_3;
  int ssubll_overflow_overflow_3 = __builtin_ssubll_overflow(long_long_min, 0LL, &ssubll_overflow_result_3);

  // __builtin_ssubll_overflow(-9223372036854775808LL, 0LL, &ssubll_overflow_result_3) stores -9223372036854775808LL.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_result_3 != long_long_min);

  // __builtin_ssubll_overflow(-9223372036854775808LL, 0LL, &ssubll_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_overflow_3 != 0);


  long long int ssubll_overflow_result_4;
  int ssubll_overflow_overflow_4 = __builtin_ssubll_overflow(long_long_min, -1LL, &ssubll_overflow_result_4);

  // __builtin_ssubll_overflow(-9223372036854775808LL, -1LL, &ssubll_overflow_result_4) stores -9223372036854775808LL + 1LL.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_result_4 != -long_long_max);

  // __builtin_ssubll_overflow(-9223372036854775808LL, -1LL, &ssubll_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_overflow_4 != 0);


  long long int ssubll_overflow_result_5;
  int ssubll_overflow_overflow_5 = __builtin_ssubll_overflow(long_long_max, 1LL, &ssubll_overflow_result_5);

  // __builtin_ssubll_overflow(9223372036854775807LL, 1LL, &ssubll_overflow_result_5) stores 9223372036854775807LL - 1LL.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_result_5 != 9223372036854775806LL);

  // __builtin_ssubll_overflow(9223372036854775807LL, 1LL, &ssubll_overflow_result_5) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_overflow_5 != 0);


  long long int ssubll_overflow_result_6;
  int ssubll_overflow_overflow_6 = __builtin_ssubll_overflow(long_long_max, -1LL, &ssubll_overflow_result_6);

  // __builtin_ssubll_overflow(9223372036854775807LL, -1LL, &ssubll_overflow_result_6) stores -9223372036854775808LL.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_result_6 != long_long_min);

  // __builtin_ssubll_overflow(9223372036854775807LL, -1LL, &ssubll_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_overflow_6 != 1);


  long long int ssubll_overflow_result_7;
  int ssubll_overflow_overflow_7 = __builtin_ssubll_overflow(long_long_min, 1LL, &ssubll_overflow_result_7);

  // __builtin_ssubll_overflow(-9223372036854775808LL, 1LL, &ssubll_overflow_result_7) stores 9223372036854775807LL.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_result_7 != long_long_max);

  // __builtin_ssubll_overflow(-9223372036854775808LL, 1LL, &ssubll_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_overflow_7 != 1);


  long long int ssubll_overflow_result_8;
  int ssubll_overflow_overflow_8 = __builtin_ssubll_overflow(0LL, long_long_min, &ssubll_overflow_result_8);

  // __builtin_ssubll_overflow(0LL, -9223372036854775808LL, &ssubll_overflow_result_8) stores -9223372036854775808LL.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_result_8 != long_long_min);

  // __builtin_ssubll_overflow(0LL, -9223372036854775808LL, &ssubll_overflow_result_8) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (ssubll_overflow_overflow_8 != 1);


  // Tests for __builtin_usub_overflow.

  unsigned int usub_overflow_result_1;
  int usub_overflow_overflow_1 = __builtin_usub_overflow(0U, 0U, &usub_overflow_result_1);

  // __builtin_usub_overflow(0U, 0U, &usub_overflow_result_1) stores 0U.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_result_1 != 0U);

  // __builtin_usub_overflow(0U, 0U, &usub_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_overflow_1 != 0);


  unsigned int usub_overflow_result_2;
  int usub_overflow_overflow_2 = __builtin_usub_overflow(unsigned_int_max, 0U, &usub_overflow_result_2);

  // __builtin_usub_overflow(4294967295U, 0U, &usub_overflow_result_2) stores 4294967295U.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_result_2 != unsigned_int_max);

  // __builtin_usub_overflow(4294967295U, 0U, &usub_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_overflow_2 != 0);


  unsigned int usub_overflow_result_3;
  int usub_overflow_overflow_3 = __builtin_usub_overflow(unsigned_int_max, unsigned_int_max, &usub_overflow_result_3);

  // __builtin_usub_overflow(4294967295U, 4294967295U, &usub_overflow_result_3) stores 0U.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_result_3 != 0U);

  // __builtin_usub_overflow(4294967295U, 4294967295U, &usub_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_overflow_3 != 0);


  unsigned int usub_overflow_result_4;
  int usub_overflow_overflow_4 = __builtin_usub_overflow(1U, 1U, &usub_overflow_result_4);

  // __builtin_usub_overflow(1U, 1U, &usub_overflow_result_4) stores 0U.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_result_4 != 0U);

  // __builtin_usub_overflow(1U, 1U, &usub_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_overflow_4 != 0);


  unsigned int usub_overflow_result_5;
  int usub_overflow_overflow_5 = __builtin_usub_overflow(0U, 1U, &usub_overflow_result_5);

  // __builtin_usub_overflow(0U, 1U, &usub_overflow_result_5) stores 4294967295U.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_result_5 != unsigned_int_max);

  // __builtin_usub_overflow(0U, 1U, &usub_overflow_result_5) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_overflow_5 != 1);


  unsigned int usub_overflow_result_6;
  int usub_overflow_overflow_6 = __builtin_usub_overflow(0U, unsigned_int_max, &usub_overflow_result_6);

  // __builtin_usub_overflow(0U, 4294967295U, &usub_overflow_result_6) stores 1U.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_result_6 != 1U);

  // __builtin_usub_overflow(0U, 4294967295U, &usub_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_overflow_6 != 1);


  unsigned int usub_overflow_result_7;
  int usub_overflow_overflow_7 = __builtin_usub_overflow(1U, 2U, &usub_overflow_result_7);

  // __builtin_usub_overflow(1U, 2U, &usub_overflow_result_7) stores 4294967295U.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_result_7 != unsigned_int_max);

  // __builtin_usub_overflow(1U, 2U, &usub_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (usub_overflow_overflow_7 != 1);


  // Tests for __builtin_usubl_overflow.

  const unsigned long int usubl_overflow_unsigned_long_max = ~0UL;


  unsigned long int usubl_overflow_result_1;
  int usubl_overflow_overflow_1 = __builtin_usubl_overflow(0UL, 0UL, &usubl_overflow_result_1);

  // __builtin_usubl_overflow(0UL, 0UL, &usubl_overflow_result_1) stores 0UL.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_result_1 != 0UL);

  // __builtin_usubl_overflow(0UL, 0UL, &usubl_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_overflow_1 != 0);


  unsigned long int usubl_overflow_result_2;
  int usubl_overflow_overflow_2 = __builtin_usubl_overflow(usubl_overflow_unsigned_long_max, 0UL, &usubl_overflow_result_2);

  // ILP32: __builtin_usubl_overflow(4294967295UL, 0UL, &usubl_overflow_result_2) stores 4294967295UL.
  // LP64: __builtin_usubl_overflow(18446744073709551615UL, 0UL, &usubl_overflow_result_2) stores 18446744073709551615UL.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_result_2 != usubl_overflow_unsigned_long_max);

  // ILP32: __builtin_usubl_overflow(4294967295UL, 0UL, &usubl_overflow_result_2) returns 0.
  // LP64: __builtin_usubl_overflow(18446744073709551615UL, 0UL, &usubl_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_overflow_2 != 0);


  unsigned long int usubl_overflow_result_3;
  int usubl_overflow_overflow_3 = __builtin_usubl_overflow(usubl_overflow_unsigned_long_max, usubl_overflow_unsigned_long_max, &usubl_overflow_result_3);

  // ILP32: __builtin_usubl_overflow(4294967295UL, 4294967295UL, &usubl_overflow_result_3) stores 0UL.
  // LP64: __builtin_usubl_overflow(18446744073709551615UL, 18446744073709551615UL, &usubl_overflow_result_3) stores 0UL.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_result_3 != 0UL);

  // ILP32: __builtin_usubl_overflow(4294967295UL, 4294967295UL, &usubl_overflow_result_3) returns 0.
  // LP64: __builtin_usubl_overflow(18446744073709551615UL, 18446744073709551615UL, &usubl_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_overflow_3 != 0);


  unsigned long int usubl_overflow_result_4;
  int usubl_overflow_overflow_4 = __builtin_usubl_overflow(1UL, 1UL, &usubl_overflow_result_4);

  // __builtin_usubl_overflow(1UL, 1UL, &usubl_overflow_result_4) stores 0UL.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_result_4 != 0UL);

  // __builtin_usubl_overflow(1UL, 1UL, &usubl_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_overflow_4 != 0);


  unsigned long int usubl_overflow_result_5;
  int usubl_overflow_overflow_5 = __builtin_usubl_overflow(0UL, 1UL, &usubl_overflow_result_5);

  // ILP32: __builtin_usubl_overflow(0UL, 1UL, &usubl_overflow_result_5) stores 4294967295UL.
  // LP64: __builtin_usubl_overflow(0UL, 1UL, &usubl_overflow_result_5) stores 18446744073709551615UL.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_result_5 != usubl_overflow_unsigned_long_max);

  // __builtin_usubl_overflow(0UL, 1UL, &usubl_overflow_result_5) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_overflow_5 != 1);


  unsigned long int usubl_overflow_result_6;
  int usubl_overflow_overflow_6 = __builtin_usubl_overflow(0UL, usubl_overflow_unsigned_long_max, &usubl_overflow_result_6);

  // ILP32: __builtin_usubl_overflow(0UL, 4294967295UL, &usubl_overflow_result_6) stores 1UL.
  // LP64: __builtin_usubl_overflow(0UL, 18446744073709551615UL, &usubl_overflow_result_6) stores 1UL.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_result_6 != 1UL);

  // ILP32: __builtin_usubl_overflow(0UL, 4294967295UL, &usubl_overflow_result_6) returns 1.
  // LP64: __builtin_usubl_overflow(0UL, 18446744073709551615UL, &usubl_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_overflow_6 != 1);


  unsigned long int usubl_overflow_result_7;
  int usubl_overflow_overflow_7 = __builtin_usubl_overflow(1UL, 2UL, &usubl_overflow_result_7);

  // ILP32: __builtin_usubl_overflow(1UL, 2UL, &usubl_overflow_result_7) stores 4294967295UL.
  // LP64: __builtin_usubl_overflow(1UL, 2UL, &usubl_overflow_result_7) stores 18446744073709551615UL.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_result_7 != usubl_overflow_unsigned_long_max);

  // __builtin_usubl_overflow(1UL, 2UL, &usubl_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubl_overflow_overflow_7 != 1);


  // Tests for __builtin_usubll_overflow.

  unsigned long long int usubll_overflow_result_1;
  int usubll_overflow_overflow_1 = __builtin_usubll_overflow(0ULL, 0ULL, &usubll_overflow_result_1);

  // __builtin_usubll_overflow(0ULL, 0ULL, &usubll_overflow_result_1) stores 0ULL.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_result_1 != 0ULL);

  // __builtin_usubll_overflow(0ULL, 0ULL, &usubll_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_overflow_1 != 0);


  unsigned long long int usubll_overflow_result_2;
  int usubll_overflow_overflow_2 = __builtin_usubll_overflow(unsigned_long_long_max, 0ULL, &usubll_overflow_result_2);

  // __builtin_usubll_overflow(18446744073709551615ULL, 0ULL, &usubll_overflow_result_2) stores 18446744073709551615ULL.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_result_2 != unsigned_long_long_max);

  // __builtin_usubll_overflow(18446744073709551615ULL, 0ULL, &usubll_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_overflow_2 != 0);


  unsigned long long int usubll_overflow_result_3;
  int usubll_overflow_overflow_3 = __builtin_usubll_overflow(unsigned_long_long_max, unsigned_long_long_max, &usubll_overflow_result_3);

  // __builtin_usubll_overflow(18446744073709551615ULL, 18446744073709551615ULL, &usubll_overflow_result_3) stores 0ULL.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_result_3 != 0ULL);

  // __builtin_usubll_overflow(18446744073709551615ULL, 18446744073709551615ULL, &usubll_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_overflow_3 != 0);


  unsigned long long int usubll_overflow_result_4;
  int usubll_overflow_overflow_4 = __builtin_usubll_overflow(1ULL, 1ULL, &usubll_overflow_result_4);

  // __builtin_usubll_overflow(1ULL, 1ULL, &usubll_overflow_result_4) stores 0ULL.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_result_4 != 0ULL);

  // __builtin_usubll_overflow(1ULL, 1ULL, &usubll_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_overflow_4 != 0);


  unsigned long long int usubll_overflow_result_5;
  int usubll_overflow_overflow_5 = __builtin_usubll_overflow(0ULL, 1ULL, &usubll_overflow_result_5);

  // __builtin_usubll_overflow(0ULL, 1ULL, &usubll_overflow_result_5) stores 18446744073709551615ULL.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_result_5 != unsigned_long_long_max);

  // __builtin_usubll_overflow(0ULL, 1ULL, &usubll_overflow_result_5) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_overflow_5 != 1);


  unsigned long long int usubll_overflow_result_6;
  int usubll_overflow_overflow_6 = __builtin_usubll_overflow(0ULL, unsigned_long_long_max, &usubll_overflow_result_6);

  // __builtin_usubll_overflow(0ULL, 18446744073709551615ULL, &usubll_overflow_result_6) stores 1ULL.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_result_6 != 1ULL);

  // __builtin_usubll_overflow(0ULL, 18446744073709551615ULL, &usubll_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_overflow_6 != 1);


  unsigned long long int usubll_overflow_result_7;
  int usubll_overflow_overflow_7 = __builtin_usubll_overflow(1ULL, 2ULL, &usubll_overflow_result_7);

  // __builtin_usubll_overflow(1ULL, 2ULL, &usubll_overflow_result_7) stores 18446744073709551615ULL.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_result_7 != unsigned_long_long_max);

  // __builtin_usubll_overflow(1ULL, 2ULL, &usubll_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (usubll_overflow_overflow_7 != 1);


  // If no expected-value check fails, every deliberately negated check is false.
  if (!(all_expected_checks_fail))
    goto ERROR;

  return 0;

ERROR:
  return 1;
}
