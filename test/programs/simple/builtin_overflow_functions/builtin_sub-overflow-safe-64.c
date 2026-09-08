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
  const unsigned short int ushort_max = 65535U;
  const int int_min = (-2147483647 - 1);
  const int int_max = 2147483647;
  const unsigned int uint_max = 4294967295U;
  const long long int ll_min = (-9223372036854775807LL - 1LL);
  const long long int ll_max = 9223372036854775807LL;
  const unsigned long long int ull_max = 18446744073709551615ULL;
  const unsigned long int sub_ulong_max = ~0UL;
  const long int sub_long_max = (long int)((~0UL) >> 1);
  const long int sub_long_min = (-((long int)((~0UL) >> 1)) - 1L);
  const long int ssubl_max = (long int)((~0UL) >> 1);
  const long int ssubl_min = (-((long int)((~0UL) >> 1)) - 1L);
  const unsigned long int usubl_max = ~0UL;


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: -2147483648L - 1L fits in long, stores -2147483649L, and returns 0.
  long int model_ssubl_res;
  int model_ssubl_ov;
  model_ssubl_ov = __builtin_ssubl_overflow((-2147483647L - 1L), 1L, &model_ssubl_res);

  if (!(model_ssubl_res != 0L && model_ssubl_ov == 0))
    goto ERROR;
  signed char sub_schar_min_1_res;
  int sub_schar_min_1_ov;
  sub_schar_min_1_ov = __builtin_sub_overflow(schar_min, 1, &sub_schar_min_1_res);

  // -128 - 1 = -129, outside the destination range; stored result = 127 and overflow = 1.
  if (!(sub_schar_min_1_res == schar_max))
    goto ERROR;

  if (!(sub_schar_min_1_ov == 1))
    goto ERROR;


  unsigned char sub_0_1_res;
  int sub_0_1_ov;
  sub_0_1_ov = __builtin_sub_overflow(0, 1, &sub_0_1_res);

  // 0 - 1 = -1, outside the destination range; stored result = 255U and overflow = 1.
  if (!(sub_0_1_res == uchar_max))
    goto ERROR;

  if (!(sub_0_1_ov == 1))
    goto ERROR;


  short int sub_short_max_m1_res;
  int sub_short_max_m1_ov;
  sub_short_max_m1_ov = __builtin_sub_overflow(short_max, -1, &sub_short_max_m1_res);

  // 32767 - -1 = 32768, outside the destination range; stored result = -32768 and overflow = 1.
  if (!(sub_short_max_m1_res == short_min))
    goto ERROR;

  if (!(sub_short_max_m1_ov == 1))
    goto ERROR;


  unsigned short int sub_ushort_max_max_res;
  int sub_ushort_max_max_ov;
  sub_ushort_max_max_ov = __builtin_sub_overflow(ushort_max, ushort_max, &sub_ushort_max_max_res);

  // 65535U - 65535U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(sub_ushort_max_max_res == 0U))
    goto ERROR;

  if (!(sub_ushort_max_max_ov == 0))
    goto ERROR;


  int sub_int_min_1_res;
  int sub_int_min_1_ov;
  sub_int_min_1_ov = __builtin_sub_overflow(int_min, 1, &sub_int_min_1_res);

  // -2147483648 - 1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  if (!(sub_int_min_1_res == int_max))
    goto ERROR;

  if (!(sub_int_min_1_ov == 1))
    goto ERROR;


  unsigned int sub_uint_0_max_res;
  int sub_uint_0_max_ov;
  sub_uint_0_max_ov = __builtin_sub_overflow(0U, uint_max, &sub_uint_0_max_res);

  // 0U - 4294967295U = -4294967295, outside the destination range; stored result = 1U and overflow = 1.
  if (!(sub_uint_0_max_res == 1U))
    goto ERROR;

  if (!(sub_uint_0_max_ov == 1))
    goto ERROR;


  long int sub_long_max_m1_res;
  int sub_long_max_m1_ov;
  sub_long_max_m1_ov = __builtin_sub_overflow(sub_long_max, -1L, &sub_long_max_m1_res);

  // 9223372036854775807L - -1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  if (!(sub_long_max_m1_res == sub_long_min))
    goto ERROR;

  if (!(sub_long_max_m1_ov == 1))
    goto ERROR;


  unsigned long int sub_ulong_0_1_res;
  int sub_ulong_0_1_ov;
  sub_ulong_0_1_ov = __builtin_sub_overflow(0UL, 1UL, &sub_ulong_0_1_res);

  // 0UL - 1UL = -1, outside the destination range; stored result = 18446744073709551615UL and overflow = 1.
  if (!(sub_ulong_0_1_res == sub_ulong_max))
    goto ERROR;

  if (!(sub_ulong_0_1_ov == 1))
    goto ERROR;


  long long int sub_ll_0_min_res;
  int sub_ll_0_min_ov;
  sub_ll_0_min_ov = __builtin_sub_overflow(0LL, ll_min, &sub_ll_0_min_res);

  // 0LL - -9223372036854775808LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  if (!(sub_ll_0_min_res == ll_min))
    goto ERROR;

  if (!(sub_ll_0_min_ov == 1))
    goto ERROR;


  unsigned long long int sub_ull_max_max_res;
  int sub_ull_max_max_ov;
  sub_ull_max_max_ov = __builtin_sub_overflow(ull_max, ull_max, &sub_ull_max_max_res);

  // 18446744073709551615ULL - 18446744073709551615ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(sub_ull_max_max_res == 0ULL))
    goto ERROR;

  if (!(sub_ull_max_max_ov == 0))
    goto ERROR;


  char sub_char_min_1_res;
  int sub_char_min_1_ov;
  sub_char_min_1_ov = __builtin_sub_overflow(char_min, 1, &sub_char_min_1_res);

  // (char)-128 - 1 = -129, outside the destination range; stored result = (char)127 and overflow = 1.
  if (!(sub_char_min_1_res == char_max))
    goto ERROR;

  if (!(sub_char_min_1_ov == 1))
    goto ERROR;


  int sub_int_1_1_res;
  int sub_int_1_1_ov;
  sub_int_1_1_ov = __builtin_sub_overflow(1ULL, 1LL, &sub_int_1_1_res);

  // 1ULL - 1LL = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(sub_int_1_1_res == 0))
    goto ERROR;

  if (!(sub_int_1_1_ov == 0))
    goto ERROR;


  unsigned int sub_uint_1_0_res;
  int sub_uint_1_0_ov;
  sub_uint_1_0_ov = __builtin_sub_overflow(-1LL, 0ULL, &sub_uint_1_0_res);

  // -1LL - 0ULL = -1, outside the destination range; stored result = 4294967295U and overflow = 1.
  if (!(sub_uint_1_0_res == uint_max))
    goto ERROR;

  if (!(sub_uint_1_0_ov == 1))
    goto ERROR;


  // Signed int subtraction overflow tests.

  int ssub_0_0_res;
  int ssub_0_0_ov;
  ssub_0_0_ov = __builtin_ssub_overflow(0, 0, &ssub_0_0_res);

  // 0 - 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(ssub_0_0_res == 0))
    goto ERROR;

  if (!(ssub_0_0_ov == 0))
    goto ERROR;


  int ssub_max_0_res;
  int ssub_max_0_ov;
  ssub_max_0_ov = __builtin_ssub_overflow(int_max, 0, &ssub_max_0_res);

  // 2147483647 - 0 = 2147483647, which fits the destination range; stored result = 2147483647 and overflow = 0.
  if (!(ssub_max_0_res == int_max))
    goto ERROR;

  if (!(ssub_max_0_ov == 0))
    goto ERROR;


  int ssub_min_0_res;
  int ssub_min_0_ov;
  ssub_min_0_ov = __builtin_ssub_overflow(int_min, 0, &ssub_min_0_res);

  // -2147483648 - 0 = -2147483648, which fits the destination range; stored result = -2147483648 and overflow = 0.
  if (!(ssub_min_0_res == int_min))
    goto ERROR;

  if (!(ssub_min_0_ov == 0))
    goto ERROR;


  int ssub_min_m1_res;
  int ssub_min_m1_ov;
  ssub_min_m1_ov = __builtin_ssub_overflow(int_min, -1, &ssub_min_m1_res);

  // -2147483648 - -1 = -2147483647, which fits the destination range; stored result = -2147483647 and overflow = 0.
  if (!(ssub_min_m1_res == -int_max))
    goto ERROR;

  if (!(ssub_min_m1_ov == 0))
    goto ERROR;


  int ssub_max_1_res;
  int ssub_max_1_ov;
  ssub_max_1_ov = __builtin_ssub_overflow(int_max, 1, &ssub_max_1_res);

  // 2147483647 - 1 = 2147483646, which fits the destination range; stored result = 2147483646 and overflow = 0.
  if (!(ssub_max_1_res == 2147483646))
    goto ERROR;

  if (!(ssub_max_1_ov == 0))
    goto ERROR;


  int ssub_max_m1_res;
  int ssub_max_m1_ov;
  ssub_max_m1_ov = __builtin_ssub_overflow(int_max, -1, &ssub_max_m1_res);

  // 2147483647 - -1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(ssub_max_m1_res == int_min))
    goto ERROR;

  if (!(ssub_max_m1_ov == 1))
    goto ERROR;


  int ssub_min_1_res;
  int ssub_min_1_ov;
  ssub_min_1_ov = __builtin_ssub_overflow(int_min, 1, &ssub_min_1_res);

  // -2147483648 - 1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  if (!(ssub_min_1_res == int_max))
    goto ERROR;

  if (!(ssub_min_1_ov == 1))
    goto ERROR;


  int ssub_0_int_min_res;
  int ssub_0_int_min_ov;
  ssub_0_int_min_ov = __builtin_ssub_overflow(0, int_min, &ssub_0_int_min_res);

  // 0 - -2147483648 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(ssub_0_int_min_res == int_min))
    goto ERROR;

  if (!(ssub_0_int_min_ov == 1))
    goto ERROR;
  long int ssubl_0_0_res;
  int ssubl_0_0_ov;
  ssubl_0_0_ov = __builtin_ssubl_overflow(0L, 0L, &ssubl_0_0_res);

  // 0L - 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  if (!(ssubl_0_0_res == 0L))
    goto ERROR;

  if (!(ssubl_0_0_ov == 0))
    goto ERROR;


  long int ssubl_max_0_res;
  int ssubl_max_0_ov;
  ssubl_max_0_ov = __builtin_ssubl_overflow(ssubl_max, 0L, &ssubl_max_0_res);

  // 9223372036854775807L - 0L = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807L and overflow =
  // 0.
  if (!(ssubl_max_0_res == ssubl_max))
    goto ERROR;

  if (!(ssubl_max_0_ov == 0))
    goto ERROR;


  long int ssubl_min_0_res;
  int ssubl_min_0_ov;
  ssubl_min_0_ov = __builtin_ssubl_overflow(ssubl_min, 0L, &ssubl_min_0_res);

  // -9223372036854775808L - 0L = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808L and overflow
  // = 0.
  if (!(ssubl_min_0_res == ssubl_min))
    goto ERROR;

  if (!(ssubl_min_0_ov == 0))
    goto ERROR;


  long int ssubl_min_m1_res;
  int ssubl_min_m1_ov;
  ssubl_min_m1_ov = __builtin_ssubl_overflow(ssubl_min, -1L, &ssubl_min_m1_res);

  // -9223372036854775808L - -1L = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807L and
  // overflow = 0.
  if (!(ssubl_min_m1_res == ((-((long int)((~0UL) >> 1)) - 1L)) + 1L))
    goto ERROR;

  if (!(ssubl_min_m1_ov == 0))
    goto ERROR;


  long int ssubl_max_1_res;
  int ssubl_max_1_ov;
  ssubl_max_1_ov = __builtin_ssubl_overflow(ssubl_max, 1L, &ssubl_max_1_res);

  // ssubl_max - 1L = ssubl_max - 1L, which fits the destination range; stored result = 9223372036854775806L and
  // overflow = 0.
  if (!(ssubl_max_1_res == ((long int)((~0UL) >> 1)) - 1L))
    goto ERROR;

  if (!(ssubl_max_1_ov == 0))
    goto ERROR;


  long int ssubl_max_m1_res;
  int ssubl_max_m1_ov;
  ssubl_max_m1_ov = __builtin_ssubl_overflow(ssubl_max, -1L, &ssubl_max_m1_res);

  // 9223372036854775807L - -1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  if (!(ssubl_max_m1_res == ssubl_min))
    goto ERROR;

  if (!(ssubl_max_m1_ov == 1))
    goto ERROR;


  long int ssubl_min_1_res;
  int ssubl_min_1_ov;
  ssubl_min_1_ov = __builtin_ssubl_overflow(ssubl_min, 1L, &ssubl_min_1_res);

  // -9223372036854775808L - 1L = -9223372036854775809, outside the destination range; stored result = 9223372036854775807L and overflow =
  // 1.
  if (!(ssubl_min_1_res == ssubl_max))
    goto ERROR;

  if (!(ssubl_min_1_ov == 1))
    goto ERROR;


  long int ssubl_0_long_min_res;
  int ssubl_0_long_min_ov;
  ssubl_0_long_min_ov = __builtin_ssubl_overflow(0L, ssubl_min, &ssubl_0_long_min_res);

  // 0L - -9223372036854775808L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  if (!(ssubl_0_long_min_res == ssubl_min))
    goto ERROR;

  if (!(ssubl_0_long_min_ov == 1))
    goto ERROR;


  // Signed long long int subtraction overflow tests.

  long long int ssubll_0_0_res;
  int ssubll_0_0_ov;
  ssubll_0_0_ov = __builtin_ssubll_overflow(0LL, 0LL, &ssubll_0_0_res);

  // 0LL - 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  if (!(ssubll_0_0_res == 0LL))
    goto ERROR;

  if (!(ssubll_0_0_ov == 0))
    goto ERROR;


  long long int ssubll_max_0_res;
  int ssubll_max_0_ov;
  ssubll_max_0_ov = __builtin_ssubll_overflow(ll_max, 0LL, &ssubll_max_0_res);

  // 9223372036854775807LL - 0LL = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807LL and overflow
  // = 0.
  if (!(ssubll_max_0_res == ll_max))
    goto ERROR;

  if (!(ssubll_max_0_ov == 0))
    goto ERROR;


  long long int ssubll_min_0_res;
  int ssubll_min_0_ov;
  ssubll_min_0_ov = __builtin_ssubll_overflow(ll_min, 0LL, &ssubll_min_0_res);

  // -9223372036854775808LL - 0LL = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808LL and
  // overflow = 0.
  if (!(ssubll_min_0_res == ll_min))
    goto ERROR;

  if (!(ssubll_min_0_ov == 0))
    goto ERROR;


  long long int ssubll_min_m1_res;
  int ssubll_min_m1_ov;
  ssubll_min_m1_ov = __builtin_ssubll_overflow(ll_min, -1LL, &ssubll_min_m1_res);

  // -9223372036854775808LL - -1LL = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807LL and
  // overflow = 0.
  if (!(ssubll_min_m1_res == -ll_max))
    goto ERROR;

  if (!(ssubll_min_m1_ov == 0))
    goto ERROR;


  long long int ssubll_max_1_res;
  int ssubll_max_1_ov;
  ssubll_max_1_ov = __builtin_ssubll_overflow(ll_max, 1LL, &ssubll_max_1_res);

  // 9223372036854775807LL - 1LL = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806LL and overflow
  // = 0.
  if (!(ssubll_max_1_res == 9223372036854775806LL))
    goto ERROR;

  if (!(ssubll_max_1_ov == 0))
    goto ERROR;


  long long int ssubll_max_m1_res;
  int ssubll_max_m1_ov;
  ssubll_max_m1_ov = __builtin_ssubll_overflow(ll_max, -1LL, &ssubll_max_m1_res);

  // 9223372036854775807LL - -1LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  if (!(ssubll_max_m1_res == ll_min))
    goto ERROR;

  if (!(ssubll_max_m1_ov == 1))
    goto ERROR;


  long long int ssubll_min_1_res;
  int ssubll_min_1_ov;
  ssubll_min_1_ov = __builtin_ssubll_overflow(ll_min, 1LL, &ssubll_min_1_res);

  // -9223372036854775808LL - 1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  if (!(ssubll_min_1_res == ll_max))
    goto ERROR;

  if (!(ssubll_min_1_ov == 1))
    goto ERROR;


  long long int ssubll_0_ll_min_res;
  int ssubll_0_ll_min_ov;
  ssubll_0_ll_min_ov = __builtin_ssubll_overflow(0LL, ll_min, &ssubll_0_ll_min_res);

  // 0LL - -9223372036854775808LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  if (!(ssubll_0_ll_min_res == ll_min))
    goto ERROR;

  if (!(ssubll_0_ll_min_ov == 1))
    goto ERROR;


  // Unsigned int subtraction overflow tests.

  unsigned int usub_0_0_res;
  int usub_0_0_ov;
  usub_0_0_ov = __builtin_usub_overflow(0U, 0U, &usub_0_0_res);

  // 0U - 0U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(usub_0_0_res == 0U))
    goto ERROR;

  if (!(usub_0_0_ov == 0))
    goto ERROR;


  unsigned int usub_max_0_res;
  int usub_max_0_ov;
  usub_max_0_ov = __builtin_usub_overflow(uint_max, 0U, &usub_max_0_res);

  // 4294967295U - 0U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  if (!(usub_max_0_res == uint_max))
    goto ERROR;

  if (!(usub_max_0_ov == 0))
    goto ERROR;


  unsigned int usub_max_max_res;
  int usub_max_max_ov;
  usub_max_max_ov = __builtin_usub_overflow(uint_max, uint_max, &usub_max_max_res);

  // 4294967295U - 4294967295U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(usub_max_max_res == 0U))
    goto ERROR;

  if (!(usub_max_max_ov == 0))
    goto ERROR;


  unsigned int usub_1_1_res;
  int usub_1_1_ov;
  usub_1_1_ov = __builtin_usub_overflow(1U, 1U, &usub_1_1_res);

  // 1U - 1U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(usub_1_1_res == 0U))
    goto ERROR;

  if (!(usub_1_1_ov == 0))
    goto ERROR;


  unsigned int usub_0_1_res;
  int usub_0_1_ov;
  usub_0_1_ov = __builtin_usub_overflow(0U, 1U, &usub_0_1_res);

  // 0U - 1U = -1, outside the destination range; stored result = 4294967295U and overflow = 1.
  if (!(usub_0_1_res == uint_max))
    goto ERROR;

  if (!(usub_0_1_ov == 1))
    goto ERROR;


  unsigned int usub_0_uint_max_res;
  int usub_0_uint_max_ov;
  usub_0_uint_max_ov = __builtin_usub_overflow(0U, uint_max, &usub_0_uint_max_res);

  // 0U - 4294967295U = -4294967295, outside the destination range; stored result = 1U and overflow = 1.
  if (!(usub_0_uint_max_res == 1U))
    goto ERROR;

  if (!(usub_0_uint_max_ov == 1))
    goto ERROR;


  unsigned int usub_1_2_res;
  int usub_1_2_ov;
  usub_1_2_ov = __builtin_usub_overflow(1U, 2U, &usub_1_2_res);

  // 1U - 2U = -1, outside the destination range; stored result = 4294967295U and overflow = 1.
  if (!(usub_1_2_res == uint_max))
    goto ERROR;

  if (!(usub_1_2_ov == 1))
    goto ERROR;
  unsigned long int usubl_0_0_res;
  int usubl_0_0_ov;
  usubl_0_0_ov = __builtin_usubl_overflow(0UL, 0UL, &usubl_0_0_res);

  // 0UL - 0UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  if (!(usubl_0_0_res == 0UL))
    goto ERROR;

  if (!(usubl_0_0_ov == 0))
    goto ERROR;


  unsigned long int usubl_max_0_res;
  int usubl_max_0_ov;
  usubl_max_0_ov = __builtin_usubl_overflow(usubl_max, 0UL, &usubl_max_0_res);

  // 18446744073709551615UL - 0UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  if (!(usubl_max_0_res == usubl_max))
    goto ERROR;

  if (!(usubl_max_0_ov == 0))
    goto ERROR;


  unsigned long int usubl_max_max_res;
  int usubl_max_max_ov;
  usubl_max_max_ov = __builtin_usubl_overflow(usubl_max, usubl_max, &usubl_max_max_res);

  // 18446744073709551615UL - 18446744073709551615UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  if (!(usubl_max_max_res == 0UL))
    goto ERROR;

  if (!(usubl_max_max_ov == 0))
    goto ERROR;


  unsigned long int usubl_1_1_res;
  int usubl_1_1_ov;
  usubl_1_1_ov = __builtin_usubl_overflow(1UL, 1UL, &usubl_1_1_res);

  // 1UL - 1UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  if (!(usubl_1_1_res == 0UL))
    goto ERROR;

  if (!(usubl_1_1_ov == 0))
    goto ERROR;


  unsigned long int usubl_0_1_res;
  int usubl_0_1_ov;
  usubl_0_1_ov = __builtin_usubl_overflow(0UL, 1UL, &usubl_0_1_res);

  // 0UL - 1UL = -1, outside the destination range; stored result = 18446744073709551615UL and overflow = 1.
  if (!(usubl_0_1_res == usubl_max))
    goto ERROR;

  if (!(usubl_0_1_ov == 1))
    goto ERROR;


  unsigned long int usubl_0_ulong_max_res;
  int usubl_0_ulong_max_ov;
  usubl_0_ulong_max_ov = __builtin_usubl_overflow(0UL, usubl_max, &usubl_0_ulong_max_res);

  // 0UL - 18446744073709551615UL = -18446744073709551615, outside the destination range; stored result = 1UL and overflow = 1.
  if (!(usubl_0_ulong_max_res == 1UL))
    goto ERROR;

  if (!(usubl_0_ulong_max_ov == 1))
    goto ERROR;


  unsigned long int usubl_1_2_res;
  int usubl_1_2_ov;
  usubl_1_2_ov = __builtin_usubl_overflow(1UL, 2UL, &usubl_1_2_res);

  // 1UL - 2UL = -1, outside the destination range; stored result = 18446744073709551615UL and overflow = 1.
  if (!(usubl_1_2_res == usubl_max))
    goto ERROR;

  if (!(usubl_1_2_ov == 1))
    goto ERROR;


  // Unsigned long long int subtraction overflow tests.

  unsigned long long int usubll_0_0_res;
  int usubll_0_0_ov;
  usubll_0_0_ov = __builtin_usubll_overflow(0ULL, 0ULL, &usubll_0_0_res);

  // 0ULL - 0ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(usubll_0_0_res == 0ULL))
    goto ERROR;

  if (!(usubll_0_0_ov == 0))
    goto ERROR;


  unsigned long long int usubll_max_0_res;
  int usubll_max_0_ov;
  usubll_max_0_ov = __builtin_usubll_overflow(ull_max, 0ULL, &usubll_max_0_res);

  // 18446744073709551615ULL - 0ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  if (!(usubll_max_0_res == ull_max))
    goto ERROR;

  if (!(usubll_max_0_ov == 0))
    goto ERROR;


  unsigned long long int usubll_max_max_res;
  int usubll_max_max_ov;
  usubll_max_max_ov = __builtin_usubll_overflow(ull_max, ull_max, &usubll_max_max_res);

  // 18446744073709551615ULL - 18446744073709551615ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(usubll_max_max_res == 0ULL))
    goto ERROR;

  if (!(usubll_max_max_ov == 0))
    goto ERROR;


  unsigned long long int usubll_1_1_res;
  int usubll_1_1_ov;
  usubll_1_1_ov = __builtin_usubll_overflow(1ULL, 1ULL, &usubll_1_1_res);

  // 1ULL - 1ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(usubll_1_1_res == 0ULL))
    goto ERROR;

  if (!(usubll_1_1_ov == 0))
    goto ERROR;


  unsigned long long int usubll_0_1_res;
  int usubll_0_1_ov;
  usubll_0_1_ov = __builtin_usubll_overflow(0ULL, 1ULL, &usubll_0_1_res);

  // 0ULL - 1ULL = -1, outside the destination range; stored result = 18446744073709551615ULL and overflow = 1.
  if (!(usubll_0_1_res == ull_max))
    goto ERROR;

  if (!(usubll_0_1_ov == 1))
    goto ERROR;


  unsigned long long int usubll_0_ull_max_res;
  int usubll_0_ull_max_ov;
  usubll_0_ull_max_ov = __builtin_usubll_overflow(0ULL, ull_max, &usubll_0_ull_max_res);

  // 0ULL - 18446744073709551615ULL = -18446744073709551615, outside the destination range; stored result = 1ULL and overflow = 1.
  if (!(usubll_0_ull_max_res == 1ULL))
    goto ERROR;

  if (!(usubll_0_ull_max_ov == 1))
    goto ERROR;


  unsigned long long int usubll_1_2_res;
  int usubll_1_2_ov;
  usubll_1_2_ov = __builtin_usubll_overflow(1ULL, 2ULL, &usubll_1_2_res);

  // 1ULL - 2ULL = -1, outside the destination range; stored result = 18446744073709551615ULL and overflow = 1.
  if (!(usubll_1_2_res == ull_max))
    goto ERROR;

  if (!(usubll_1_2_ov == 1))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
