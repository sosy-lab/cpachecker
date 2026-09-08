// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


int main(void) {

  // Constant test values are initialized directly and never modified.
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
  const unsigned long int add_ulong_max = ~0UL;
  const long int add_long_max = (long int)((~0UL) >> 1);
  const long int add_long_min = (-((long int)((~0UL) >> 1)) - 1L);
  const long int saddl_max = (long int)((~0UL) >> 1);
  const long int saddl_min = (-((long int)((~0UL) >> 1)) - 1L);
  const unsigned long int uaddl_max = ~0UL;


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: 2147483647L + 1L = 2147483648, within long range; result = 2147483648L and overflow = 0.
  long int model_saddl_res;
  int model_saddl_ov;
  model_saddl_ov = __builtin_saddl_overflow(2147483647L, 1L, &model_saddl_res);

  if (!(model_saddl_res != 0L && model_saddl_ov == 0))
    goto ERROR;
  signed char add_schar_max_0_res;
  int add_schar_max_0_ov;
  add_schar_max_0_ov = __builtin_add_overflow(schar_max, 0, &add_schar_max_0_res);

  // 127 + 0 = 127, which fits the destination range; stored result = 127 and overflow = 0.
  if (!(add_schar_max_0_res == schar_max))
    goto ERROR;

  if (!(add_schar_max_0_ov == 0))
    goto ERROR;


  signed char add_schar_max_1_res;
  int add_schar_max_1_ov;
  add_schar_max_1_ov = __builtin_add_overflow(schar_max, 1, &add_schar_max_1_res);

  // 127 + 1 = 128, outside the destination range; stored result = -128 and overflow = 1.
  if (!(add_schar_max_1_res == schar_min))
    goto ERROR;

  if (!(add_schar_max_1_ov == 1))
    goto ERROR;


  unsigned char add_uchar_max_1_res;
  int add_uchar_max_1_ov;
  add_uchar_max_1_ov = __builtin_add_overflow(uchar_max, 1, &add_uchar_max_1_res);

  // 255U + 1 = 256, outside the destination range; stored result = 0 and overflow = 1.
  if (!(add_uchar_max_1_res == 0))
    goto ERROR;

  if (!(add_uchar_max_1_ov == 1))
    goto ERROR;


  short int add_short_min_m1_res;
  int add_short_min_m1_ov;
  add_short_min_m1_ov = __builtin_add_overflow(short_min, -1, &add_short_min_m1_res);

  // -32768 + -1 = -32769, outside the destination range; stored result = 32767 and overflow = 1.
  if (!(add_short_min_m1_res == short_max))
    goto ERROR;

  if (!(add_short_min_m1_ov == 1))
    goto ERROR;


  unsigned short int add_ushort_m1_1_res;
  int add_ushort_m1_1_ov;
  add_ushort_m1_1_ov = __builtin_add_overflow(-1, 1U, &add_ushort_m1_1_res);

  // -1 + 1U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(add_ushort_m1_1_res == 0U))
    goto ERROR;

  if (!(add_ushort_m1_1_ov == 0))
    goto ERROR;


  int add_int_max_1_res;
  int add_int_max_1_ov;
  add_int_max_1_ov = __builtin_add_overflow(int_max, 1, &add_int_max_1_res);

  // 2147483647 + 1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(add_int_max_1_res == int_min))
    goto ERROR;

  if (!(add_int_max_1_ov == 1))
    goto ERROR;


  unsigned int add_uint_max_1_res;
  int add_uint_max_1_ov;
  add_uint_max_1_ov = __builtin_add_overflow(uint_max, 1U, &add_uint_max_1_res);

  // 4294967295U + 1U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  if (!(add_uint_max_1_res == 0U))
    goto ERROR;

  if (!(add_uint_max_1_ov == 1))
    goto ERROR;


  long int add_long_max_1_res;
  int add_long_max_1_ov;
  add_long_max_1_ov = __builtin_add_overflow(add_long_max, 1L, &add_long_max_1_res);

  // 9223372036854775807L + 1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow = 1.
  if (!(add_long_max_1_res == add_long_min))
    goto ERROR;

  if (!(add_long_max_1_ov == 1))
    goto ERROR;


  unsigned long int add_ulong_max_1_res;
  int add_ulong_max_1_ov;
  add_ulong_max_1_ov = __builtin_add_overflow(add_ulong_max, 1UL, &add_ulong_max_1_res);

  // 18446744073709551615UL + 1UL = 18446744073709551616, outside the destination range; stored result = 0UL and overflow = 1.
  if (!(add_ulong_max_1_res == 0UL))
    goto ERROR;

  if (!(add_ulong_max_1_ov == 1))
    goto ERROR;


  long long int add_ll_min_m1_res;
  int add_ll_min_m1_ov;
  add_ll_min_m1_ov = __builtin_add_overflow(ll_min, -1LL, &add_ll_min_m1_res);

  // -9223372036854775808LL + -1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  if (!(add_ll_min_m1_res == ll_max))
    goto ERROR;

  if (!(add_ll_min_m1_ov == 1))
    goto ERROR;


  unsigned long long int add_ull_max_1_res;
  int add_ull_max_1_ov;
  add_ull_max_1_ov = __builtin_add_overflow(ull_max, 1ULL, &add_ull_max_1_res);

  // 18446744073709551615ULL + 1ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  if (!(add_ull_max_1_res == 0ULL))
    goto ERROR;

  if (!(add_ull_max_1_ov == 1))
    goto ERROR;


  char add_char_max_1_res;
  int add_char_max_1_ov;
  add_char_max_1_ov = __builtin_add_overflow(char_max, 1, &add_char_max_1_res);

  // (char)127 + 1 = 128, outside the destination range; stored result = (char)-128 and overflow = 1.
  if (!(add_char_max_1_res == char_min))
    goto ERROR;

  if (!(add_char_max_1_ov == 1))
    goto ERROR;


  int add_int_m1_1_res;
  int add_int_m1_1_ov;
  add_int_m1_1_ov = __builtin_add_overflow(-1LL, 1ULL, &add_int_m1_1_res);

  // -1LL + 1ULL = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(add_int_m1_1_res == 0))
    goto ERROR;

  if (!(add_int_m1_1_ov == 0))
    goto ERROR;


  int add_int_ull_max_0_res;
  int add_int_ull_max_0_ov;
  add_int_ull_max_0_ov = __builtin_add_overflow(ull_max, 0, &add_int_ull_max_0_res);

  // 18446744073709551615ULL + 0 = 18446744073709551615, outside the destination range; stored result = (int)18446744073709551615ULL and
  // overflow = 1.
  if (!(add_int_ull_max_0_res == (int)ull_max))
    goto ERROR;

  if (!(add_int_ull_max_0_ov == 1))
    goto ERROR;


  // Signed int addition overflow tests.

  int sadd_0_0_res;
  int sadd_0_0_ov;
  sadd_0_0_ov = __builtin_sadd_overflow(0, 0, &sadd_0_0_res);

  // 0 + 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(sadd_0_0_res == 0))
    goto ERROR;

  if (!(sadd_0_0_ov == 0))
    goto ERROR;


  int sadd_max_0_res;
  int sadd_max_0_ov;
  sadd_max_0_ov = __builtin_sadd_overflow(int_max, 0, &sadd_max_0_res);

  // 2147483647 + 0 = 2147483647, which fits the destination range; stored result = 2147483647 and overflow = 0.
  if (!(sadd_max_0_res == int_max))
    goto ERROR;

  if (!(sadd_max_0_ov == 0))
    goto ERROR;


  int sadd_min_0_res;
  int sadd_min_0_ov;
  sadd_min_0_ov = __builtin_sadd_overflow(int_min, 0, &sadd_min_0_res);

  // -2147483648 + 0 = -2147483648, which fits the destination range; stored result = -2147483648 and overflow = 0.
  if (!(sadd_min_0_res == int_min))
    goto ERROR;

  if (!(sadd_min_0_ov == 0))
    goto ERROR;


  int sadd_max_m1_res;
  int sadd_max_m1_ov;
  sadd_max_m1_ov = __builtin_sadd_overflow(int_max, -1, &sadd_max_m1_res);

  // 2147483647 + -1 = 2147483646, which fits the destination range; stored result = 2147483646 and overflow = 0.
  if (!(sadd_max_m1_res == 2147483646))
    goto ERROR;

  if (!(sadd_max_m1_ov == 0))
    goto ERROR;


  int sadd_min_1_res;
  int sadd_min_1_ov;
  sadd_min_1_ov = __builtin_sadd_overflow(int_min, 1, &sadd_min_1_res);

  // -2147483648 + 1 = -2147483647, which fits the destination range; stored result = -2147483647 and overflow = 0.
  if (!(sadd_min_1_res == -int_max))
    goto ERROR;

  if (!(sadd_min_1_ov == 0))
    goto ERROR;


  int sadd_max_1_res;
  int sadd_max_1_ov;
  sadd_max_1_ov = __builtin_sadd_overflow(int_max, 1, &sadd_max_1_res);

  // 2147483647 + 1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(sadd_max_1_res == int_min))
    goto ERROR;

  if (!(sadd_max_1_ov == 1))
    goto ERROR;


  int sadd_min_m1_res;
  int sadd_min_m1_ov;
  sadd_min_m1_ov = __builtin_sadd_overflow(int_min, -1, &sadd_min_m1_res);

  // -2147483648 + -1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  if (!(sadd_min_m1_res == int_max))
    goto ERROR;

  if (!(sadd_min_m1_ov == 1))
    goto ERROR;
  long int saddl_0_0_res;
  int saddl_0_0_ov;
  saddl_0_0_ov = __builtin_saddl_overflow(0L, 0L, &saddl_0_0_res);

  // 0L + 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  if (!(saddl_0_0_res == 0L))
    goto ERROR;

  if (!(saddl_0_0_ov == 0))
    goto ERROR;


  long int saddl_max_0_res;
  int saddl_max_0_ov;
  saddl_max_0_ov = __builtin_saddl_overflow(saddl_max, 0L, &saddl_max_0_res);

  // 9223372036854775807L + 0L = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807L and overflow =
  // 0.
  if (!(saddl_max_0_res == saddl_max))
    goto ERROR;

  if (!(saddl_max_0_ov == 0))
    goto ERROR;


  long int saddl_min_0_res;
  int saddl_min_0_ov;
  saddl_min_0_ov = __builtin_saddl_overflow(saddl_min, 0L, &saddl_min_0_res);

  // -9223372036854775808L + 0L = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808L and overflow
  // = 0.
  if (!(saddl_min_0_res == saddl_min))
    goto ERROR;

  if (!(saddl_min_0_ov == 0))
    goto ERROR;


  long int saddl_max_m1_res;
  int saddl_max_m1_ov;
  saddl_max_m1_ov = __builtin_saddl_overflow(saddl_max, -1L, &saddl_max_m1_res);

  // 9223372036854775807L + -1L = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806L and overflow =
  // 0.
  if (!(saddl_max_m1_res == ((long int)((~0UL) >> 1)) - 1L))
    goto ERROR;

  if (!(saddl_max_m1_ov == 0))
    goto ERROR;


  long int saddl_min_1_res;
  int saddl_min_1_ov;
  saddl_min_1_ov = __builtin_saddl_overflow(saddl_min, 1L, &saddl_min_1_res);

  // saddl_min + 1L = saddl_min + 1L, which fits the destination range; stored result = -9223372036854775807L
  // and overflow = 0.
  if (!(saddl_min_1_res == ((-((long int)((~0UL) >> 1)) - 1L)) + 1L))
    goto ERROR;

  if (!(saddl_min_1_ov == 0))
    goto ERROR;


  long int saddl_max_1_res;
  int saddl_max_1_ov;
  saddl_max_1_ov = __builtin_saddl_overflow(saddl_max, 1L, &saddl_max_1_res);

  // 9223372036854775807L + 1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow = 1.
  if (!(saddl_max_1_res == saddl_min))
    goto ERROR;

  if (!(saddl_max_1_ov == 1))
    goto ERROR;


  long int saddl_min_m1_res;
  int saddl_min_m1_ov;
  saddl_min_m1_ov = __builtin_saddl_overflow(saddl_min, -1L, &saddl_min_m1_res);

  // -9223372036854775808L + -1L = -9223372036854775809, outside the destination range; stored result = 9223372036854775807L and overflow =
  // 1.
  if (!(saddl_min_m1_res == saddl_max))
    goto ERROR;

  if (!(saddl_min_m1_ov == 1))
    goto ERROR;


  // Signed long long int addition overflow tests.

  long long int saddll_0_0_res;
  int saddll_0_0_ov;
  saddll_0_0_ov = __builtin_saddll_overflow(0LL, 0LL, &saddll_0_0_res);

  // 0LL + 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  if (!(saddll_0_0_res == 0LL))
    goto ERROR;

  if (!(saddll_0_0_ov == 0))
    goto ERROR;


  long long int saddll_max_0_res;
  int saddll_max_0_ov;
  saddll_max_0_ov = __builtin_saddll_overflow(ll_max, 0LL, &saddll_max_0_res);

  // 9223372036854775807LL + 0LL = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807LL and overflow
  // = 0.
  if (!(saddll_max_0_res == ll_max))
    goto ERROR;

  if (!(saddll_max_0_ov == 0))
    goto ERROR;


  long long int saddll_min_0_res;
  int saddll_min_0_ov;
  saddll_min_0_ov = __builtin_saddll_overflow(ll_min, 0LL, &saddll_min_0_res);

  // -9223372036854775808LL + 0LL = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808LL and
  // overflow = 0.
  if (!(saddll_min_0_res == ll_min))
    goto ERROR;

  if (!(saddll_min_0_ov == 0))
    goto ERROR;


  long long int saddll_max_m1_res;
  int saddll_max_m1_ov;
  saddll_max_m1_ov = __builtin_saddll_overflow(ll_max, -1LL, &saddll_max_m1_res);

  // 9223372036854775807LL + -1LL = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806LL and
  // overflow = 0.
  if (!(saddll_max_m1_res == 9223372036854775806LL))
    goto ERROR;

  if (!(saddll_max_m1_ov == 0))
    goto ERROR;


  long long int saddll_min_1_res;
  int saddll_min_1_ov;
  saddll_min_1_ov = __builtin_saddll_overflow(ll_min, 1LL, &saddll_min_1_res);

  // -9223372036854775808LL + 1LL = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807LL and
  // overflow = 0.
  if (!(saddll_min_1_res == -ll_max))
    goto ERROR;

  if (!(saddll_min_1_ov == 0))
    goto ERROR;


  long long int saddll_max_1_res;
  int saddll_max_1_ov;
  saddll_max_1_ov = __builtin_saddll_overflow(ll_max, 1LL, &saddll_max_1_res);

  // 9223372036854775807LL + 1LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow =
  // 1.
  if (!(saddll_max_1_res == ll_min))
    goto ERROR;

  if (!(saddll_max_1_ov == 1))
    goto ERROR;


  long long int saddll_min_m1_res;
  int saddll_min_m1_ov;
  saddll_min_m1_ov = __builtin_saddll_overflow(ll_min, -1LL, &saddll_min_m1_res);

  // -9223372036854775808LL + -1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  if (!(saddll_min_m1_res == ll_max))
    goto ERROR;

  if (!(saddll_min_m1_ov == 1))
    goto ERROR;


  // Unsigned int addition overflow tests.

  unsigned int uadd_0_0_res;
  int uadd_0_0_ov;
  uadd_0_0_ov = __builtin_uadd_overflow(0U, 0U, &uadd_0_0_res);

  // 0U + 0U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(uadd_0_0_res == 0U))
    goto ERROR;

  if (!(uadd_0_0_ov == 0))
    goto ERROR;


  unsigned int uadd_max_0_res;
  int uadd_max_0_ov;
  uadd_max_0_ov = __builtin_uadd_overflow(uint_max, 0U, &uadd_max_0_res);

  // 4294967295U + 0U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  if (!(uadd_max_0_res == uint_max))
    goto ERROR;

  if (!(uadd_max_0_ov == 0))
    goto ERROR;


  unsigned int uadd_maxm1_1_res;
  int uadd_maxm1_1_ov;
  uadd_maxm1_1_ov = __builtin_uadd_overflow(4294967294U, 1U, &uadd_maxm1_1_res);

  // 4294967294U + 1U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  if (!(uadd_maxm1_1_res == uint_max))
    goto ERROR;

  if (!(uadd_maxm1_1_ov == 0))
    goto ERROR;


  unsigned int uadd_max_1_res;
  int uadd_max_1_ov;
  uadd_max_1_ov = __builtin_uadd_overflow(uint_max, 1U, &uadd_max_1_res);

  // 4294967295U + 1U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  if (!(uadd_max_1_res == 0U))
    goto ERROR;

  if (!(uadd_max_1_ov == 1))
    goto ERROR;


  unsigned int uadd_max_2_res;
  int uadd_max_2_ov;
  uadd_max_2_ov = __builtin_uadd_overflow(uint_max, 2U, &uadd_max_2_res);

  // 4294967295U + 2U = 4294967297, outside the destination range; stored result = 1U and overflow = 1.
  if (!(uadd_max_2_res == 1U))
    goto ERROR;

  if (!(uadd_max_2_ov == 1))
    goto ERROR;


  unsigned int uadd_max_max_res;
  int uadd_max_max_ov;
  uadd_max_max_ov = __builtin_uadd_overflow(uint_max, uint_max, &uadd_max_max_res);

  // 4294967295U + 4294967295U = 8589934590, outside the destination range; stored result = 4294967294U and overflow = 1.
  if (!(uadd_max_max_res == 4294967294U))
    goto ERROR;

  if (!(uadd_max_max_ov == 1))
    goto ERROR;
  unsigned long int uaddl_0_0_res;
  int uaddl_0_0_ov;
  uaddl_0_0_ov = __builtin_uaddl_overflow(0UL, 0UL, &uaddl_0_0_res);

  // 0UL + 0UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  if (!(uaddl_0_0_res == 0UL))
    goto ERROR;

  if (!(uaddl_0_0_ov == 0))
    goto ERROR;


  unsigned long int uaddl_max_0_res;
  int uaddl_max_0_ov;
  uaddl_max_0_ov = __builtin_uaddl_overflow(uaddl_max, 0UL, &uaddl_max_0_res);

  // 18446744073709551615UL + 0UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  if (!(uaddl_max_0_res == uaddl_max))
    goto ERROR;

  if (!(uaddl_max_0_ov == 0))
    goto ERROR;


  unsigned long int uaddl_maxm1_1_res;
  int uaddl_maxm1_1_ov;
  uaddl_maxm1_1_ov = __builtin_uaddl_overflow((~0UL) - 1UL, 1UL, &uaddl_maxm1_1_res);

  // 18446744073709551614UL + 1UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  if (!(uaddl_maxm1_1_res == uaddl_max))
    goto ERROR;

  if (!(uaddl_maxm1_1_ov == 0))
    goto ERROR;


  unsigned long int uaddl_max_1_res;
  int uaddl_max_1_ov;
  uaddl_max_1_ov = __builtin_uaddl_overflow(uaddl_max, 1UL, &uaddl_max_1_res);

  // 18446744073709551615UL + 1UL = 18446744073709551616, outside the destination range; stored result = 0UL and overflow = 1.
  if (!(uaddl_max_1_res == 0UL))
    goto ERROR;

  if (!(uaddl_max_1_ov == 1))
    goto ERROR;


  unsigned long int uaddl_max_2_res;
  int uaddl_max_2_ov;
  uaddl_max_2_ov = __builtin_uaddl_overflow(uaddl_max, 2UL, &uaddl_max_2_res);

  // 18446744073709551615UL + 2UL = 18446744073709551617, outside the destination range; stored result = 1UL and overflow = 1.
  if (!(uaddl_max_2_res == 1UL))
    goto ERROR;

  if (!(uaddl_max_2_ov == 1))
    goto ERROR;


  unsigned long int uaddl_max_max_res;
  int uaddl_max_max_ov;
  uaddl_max_max_ov = __builtin_uaddl_overflow(uaddl_max, uaddl_max, &uaddl_max_max_res);

  // 18446744073709551615UL + 18446744073709551615UL = 36893488147419103230, outside the destination range; stored result =
  // 18446744073709551614UL and overflow = 1.
  if (!(uaddl_max_max_res == (~0UL) - 1UL))
    goto ERROR;

  if (!(uaddl_max_max_ov == 1))
    goto ERROR;


  // Unsigned long long int addition overflow tests.

  unsigned long long int uaddll_0_0_res;
  int uaddll_0_0_ov;
  uaddll_0_0_ov = __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_0_0_res);

  // 0ULL + 0ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(uaddll_0_0_res == 0ULL))
    goto ERROR;

  if (!(uaddll_0_0_ov == 0))
    goto ERROR;


  unsigned long long int uaddll_max_0_res;
  int uaddll_max_0_ov;
  uaddll_max_0_ov = __builtin_uaddll_overflow(ull_max, 0ULL, &uaddll_max_0_res);

  // 18446744073709551615ULL + 0ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  if (!(uaddll_max_0_res == ull_max))
    goto ERROR;

  if (!(uaddll_max_0_ov == 0))
    goto ERROR;


  unsigned long long int uaddll_maxm1_1_res;
  int uaddll_maxm1_1_ov;
  uaddll_maxm1_1_ov = __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_maxm1_1_res);

  // 18446744073709551614ULL + 1ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  if (!(uaddll_maxm1_1_res == ull_max))
    goto ERROR;

  if (!(uaddll_maxm1_1_ov == 0))
    goto ERROR;


  unsigned long long int uaddll_max_1_res;
  int uaddll_max_1_ov;
  uaddll_max_1_ov = __builtin_uaddll_overflow(ull_max, 1ULL, &uaddll_max_1_res);

  // 18446744073709551615ULL + 1ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  if (!(uaddll_max_1_res == 0ULL))
    goto ERROR;

  if (!(uaddll_max_1_ov == 1))
    goto ERROR;


  unsigned long long int uaddll_max_2_res;
  int uaddll_max_2_ov;
  uaddll_max_2_ov = __builtin_uaddll_overflow(ull_max, 2ULL, &uaddll_max_2_res);

  // 18446744073709551615ULL + 2ULL = 18446744073709551617, outside the destination range; stored result = 1ULL and overflow = 1.
  if (!(uaddll_max_2_res == 1ULL))
    goto ERROR;

  if (!(uaddll_max_2_ov == 1))
    goto ERROR;


  unsigned long long int uaddll_max_max_res;
  int uaddll_max_max_ov;
  uaddll_max_max_ov = __builtin_uaddll_overflow(ull_max, ull_max, &uaddll_max_max_res);

  // 18446744073709551615ULL + 18446744073709551615ULL = 36893488147419103230, outside the destination range; stored result =
  // 18446744073709551614ULL and overflow = 1.
  if (!(uaddll_max_max_res == 18446744073709551614ULL))
    goto ERROR;

  if (!(uaddll_max_max_ov == 1))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
