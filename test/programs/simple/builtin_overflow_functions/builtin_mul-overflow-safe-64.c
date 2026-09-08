// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


int main(void) {

  // Constant test values are initialized directly and never modified.
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
  const unsigned long int mul_ulong_max = ~0UL;
  const long int mul_long_max = (long int)((~0UL) >> 1);
  const long int smull_max = (long int)((~0UL) >> 1);
  const long int smull_min = (-((long int)((~0UL) >> 1)) - 1L);
  const unsigned long int umull_max = ~0UL;


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: 2147483647L * 2L fits in long, stores 4294967294L, and returns 0.
  long int model_smull_res;
  int model_smull_ov;
  model_smull_ov = __builtin_smull_overflow(2147483647L, 2L, &model_smull_res);

  if (!(model_smull_res != 0L && model_smull_ov == 0))
    goto ERROR;
  signed char mul_schar_max_1_res;
  int mul_schar_max_1_ov;
  mul_schar_max_1_ov = __builtin_mul_overflow(schar_max, 1, &mul_schar_max_1_res);

  // 127 * 1 = 127, which fits the destination range; stored result = 127 and overflow = 0.
  if (!(mul_schar_max_1_res == schar_max))
    goto ERROR;

  if (!(mul_schar_max_1_ov == 0))
    goto ERROR;


  signed char mul_schar_min_m1_res;
  int mul_schar_min_m1_ov;
  mul_schar_min_m1_ov = __builtin_mul_overflow(schar_min, -1, &mul_schar_min_m1_res);

  // -128 * -1 = 128, outside the destination range; stored result = -128 and overflow = 1.
  if (!(mul_schar_min_m1_res == schar_min))
    goto ERROR;

  if (!(mul_schar_min_m1_ov == 1))
    goto ERROR;


  unsigned char mul_uchar_max_max_res;
  int mul_uchar_max_max_ov;
  mul_uchar_max_max_ov = __builtin_mul_overflow(uchar_max, uchar_max, &mul_uchar_max_max_res);

  // 255U * 255U = 65025, outside the destination range; stored result = 1U and overflow = 1.
  if (!(mul_uchar_max_max_res == 1U))
    goto ERROR;

  if (!(mul_uchar_max_max_ov == 1))
    goto ERROR;


  short int mul_short_max_2_res;
  int mul_short_max_2_ov;
  mul_short_max_2_ov = __builtin_mul_overflow(short_max, 2, &mul_short_max_2_res);

  // 32767 * 2 = 65534, outside the destination range; stored result = -2 and overflow = 1.
  if (!(mul_short_max_2_res == -2))
    goto ERROR;

  if (!(mul_short_max_2_ov == 1))
    goto ERROR;


  unsigned short int mul_ushort_2_32767_res;
  int mul_ushort_2_32767_ov;
  mul_ushort_2_32767_ov = __builtin_mul_overflow(2U, 32767U, &mul_ushort_2_32767_res);

  // 2U * 32767U = 65534, which fits the destination range; stored result = 65534U and overflow = 0.
  if (!(mul_ushort_2_32767_res == 65534U))
    goto ERROR;

  if (!(mul_ushort_2_32767_ov == 0))
    goto ERROR;


  int mul_int_min_m1_res;
  int mul_int_min_m1_ov;
  mul_int_min_m1_ov = __builtin_mul_overflow(int_min, -1, &mul_int_min_m1_res);

  // -2147483648 * -1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(mul_int_min_m1_res == int_min))
    goto ERROR;

  if (!(mul_int_min_m1_ov == 1))
    goto ERROR;


  unsigned int mul_uint_max_max_res;
  int mul_uint_max_max_ov;
  mul_uint_max_max_ov = __builtin_mul_overflow(uint_max, uint_max, &mul_uint_max_max_res);

  // 4294967295U * 4294967295U = 18446744065119617025, outside the destination range; stored result = 1U and overflow = 1.
  if (!(mul_uint_max_max_res == 1U))
    goto ERROR;

  if (!(mul_uint_max_max_ov == 1))
    goto ERROR;


  long int mul_long_max_0_res;
  int mul_long_max_0_ov;
  mul_long_max_0_ov = __builtin_mul_overflow(mul_long_max, 0L, &mul_long_max_0_res);

  // 9223372036854775807L * 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  if (!(mul_long_max_0_res == 0L))
    goto ERROR;

  if (!(mul_long_max_0_ov == 0))
    goto ERROR;


  unsigned long int mul_ulong_max_2_res;
  int mul_ulong_max_2_ov;
  mul_ulong_max_2_ov = __builtin_mul_overflow(mul_ulong_max, 2UL, &mul_ulong_max_2_res);

  // 18446744073709551615UL * 2UL = 36893488147419103230, outside the destination range; stored result = 18446744073709551614UL and overflow
  // = 1.
  if (!(mul_ulong_max_2_res == (~0UL) - 1UL))
    goto ERROR;

  if (!(mul_ulong_max_2_ov == 1))
    goto ERROR;


  long long int mul_ll_min_0_res;
  int mul_ll_min_0_ov;
  mul_ll_min_0_ov = __builtin_mul_overflow(ll_min, 0LL, &mul_ll_min_0_res);

  // -9223372036854775808LL * 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  if (!(mul_ll_min_0_res == 0LL))
    goto ERROR;

  if (!(mul_ll_min_0_ov == 0))
    goto ERROR;


  unsigned long long int mul_ull_max_max_res;
  int mul_ull_max_max_ov;
  mul_ull_max_max_ov = __builtin_mul_overflow(ull_max, ull_max, &mul_ull_max_max_res);

  // 18446744073709551615ULL * 18446744073709551615ULL = 340282366920938463426481119284349108225, outside the destination range; stored
  // result = 1ULL and overflow = 1.
  if (!(mul_ull_max_max_res == 1ULL))
    goto ERROR;

  if (!(mul_ull_max_max_ov == 1))
    goto ERROR;


  char mul_char_max_2_res;
  int mul_char_max_2_ov;
  mul_char_max_2_ov = __builtin_mul_overflow(char_max, 2, &mul_char_max_2_res);

  // (char)127 * 2 = 254, outside the destination range; stored result = (char)-2 and overflow = 1.
  if (!(mul_char_max_2_res == char_2))
    goto ERROR;

  if (!(mul_char_max_2_ov == 1))
    goto ERROR;


  int mul_int_1_1_res;
  int mul_int_1_1_ov;
  mul_int_1_1_ov = __builtin_mul_overflow(-1LL, 1ULL, &mul_int_1_1_res);

  // -1LL * 1ULL = -1, which fits the destination range; stored result = -1 and overflow = 0.
  if (!(mul_int_1_1_res == -1))
    goto ERROR;

  if (!(mul_int_1_1_ov == 0))
    goto ERROR;


  unsigned int mul_uint_1_2_res;
  int mul_uint_1_2_ov;
  mul_uint_1_2_ov = __builtin_mul_overflow(-1LL, 2ULL, &mul_uint_1_2_res);

  // -1LL * 2ULL = -2, outside the destination range; stored result = 4294967294U and overflow = 1.
  if (!(mul_uint_1_2_res == 4294967294U))
    goto ERROR;

  if (!(mul_uint_1_2_ov == 1))
    goto ERROR;


  // Signed int multiplication overflow tests.

  int smul_0_0_res;
  int smul_0_0_ov;
  smul_0_0_ov = __builtin_smul_overflow(0, 0, &smul_0_0_res);

  // 0 * 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(smul_0_0_res == 0))
    goto ERROR;

  if (!(smul_0_0_ov == 0))
    goto ERROR;


  int smul_max_0_res;
  int smul_max_0_ov;
  smul_max_0_ov = __builtin_smul_overflow(int_max, 0, &smul_max_0_res);

  // 2147483647 * 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(smul_max_0_res == 0))
    goto ERROR;

  if (!(smul_max_0_ov == 0))
    goto ERROR;


  int smul_min_0_res;
  int smul_min_0_ov;
  smul_min_0_ov = __builtin_smul_overflow(int_min, 0, &smul_min_0_res);

  // -2147483648 * 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(smul_min_0_res == 0))
    goto ERROR;

  if (!(smul_min_0_ov == 0))
    goto ERROR;


  int smul_max_1_res;
  int smul_max_1_ov;
  smul_max_1_ov = __builtin_smul_overflow(int_max, 1, &smul_max_1_res);

  // 2147483647 * 1 = 2147483647, which fits the destination range; stored result = 2147483647 and overflow = 0.
  if (!(smul_max_1_res == int_max))
    goto ERROR;

  if (!(smul_max_1_ov == 0))
    goto ERROR;


  int smul_min_1_res;
  int smul_min_1_ov;
  smul_min_1_ov = __builtin_smul_overflow(int_min, 1, &smul_min_1_res);

  // -2147483648 * 1 = -2147483648, which fits the destination range; stored result = -2147483648 and overflow = 0.
  if (!(smul_min_1_res == int_min))
    goto ERROR;

  if (!(smul_min_1_ov == 0))
    goto ERROR;


  int smul_max_m1_res;
  int smul_max_m1_ov;
  smul_max_m1_ov = __builtin_smul_overflow(int_max, -1, &smul_max_m1_res);

  // 2147483647 * -1 = -2147483647, which fits the destination range; stored result = -2147483647 and overflow = 0.
  if (!(smul_max_m1_res == -int_max))
    goto ERROR;

  if (!(smul_max_m1_ov == 0))
    goto ERROR;


  int smul_min_m1_res;
  int smul_min_m1_ov;
  smul_min_m1_ov = __builtin_smul_overflow(int_min, -1, &smul_min_m1_res);

  // -2147483648 * -1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(smul_min_m1_res == int_min))
    goto ERROR;

  if (!(smul_min_m1_ov == 1))
    goto ERROR;


  int smul_max_2_res;
  int smul_max_2_ov;
  smul_max_2_ov = __builtin_smul_overflow(int_max, 2, &smul_max_2_res);

  // 2147483647 * 2 = 4294967294, outside the destination range; stored result = -2 and overflow = 1.
  if (!(smul_max_2_res == -2))
    goto ERROR;

  if (!(smul_max_2_ov == 1))
    goto ERROR;


  int smul_min_2_res;
  int smul_min_2_ov;
  smul_min_2_ov = __builtin_smul_overflow(int_min, 2, &smul_min_2_res);

  // -2147483648 * 2 = -4294967296, outside the destination range; stored result = 0 and overflow = 1.
  if (!(smul_min_2_res == 0))
    goto ERROR;

  if (!(smul_min_2_ov == 1))
    goto ERROR;


  int smul_1_m1_res;
  int smul_1_m1_ov;
  smul_1_m1_ov = __builtin_smul_overflow(-1, -1, &smul_1_m1_res);

  // -1 * -1 = 1, which fits the destination range; stored result = 1 and overflow = 0.
  if (!(smul_1_m1_res == 1))
    goto ERROR;

  if (!(smul_1_m1_ov == 0))
    goto ERROR;
  long int smull_0_0_res;
  int smull_0_0_ov;
  smull_0_0_ov = __builtin_smull_overflow(0L, 0L, &smull_0_0_res);

  // 0L * 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  if (!(smull_0_0_res == 0L))
    goto ERROR;

  if (!(smull_0_0_ov == 0))
    goto ERROR;


  long int smull_max_0_res;
  int smull_max_0_ov;
  smull_max_0_ov = __builtin_smull_overflow(smull_max, 0L, &smull_max_0_res);

  // 9223372036854775807L * 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  if (!(smull_max_0_res == 0L))
    goto ERROR;

  if (!(smull_max_0_ov == 0))
    goto ERROR;


  long int smull_min_0_res;
  int smull_min_0_ov;
  smull_min_0_ov = __builtin_smull_overflow(smull_min, 0L, &smull_min_0_res);

  // -9223372036854775808L * 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  if (!(smull_min_0_res == 0L))
    goto ERROR;

  if (!(smull_min_0_ov == 0))
    goto ERROR;


  long int smull_max_1_res;
  int smull_max_1_ov;
  smull_max_1_ov = __builtin_smull_overflow(smull_max, 1L, &smull_max_1_res);

  // 9223372036854775807L * 1L = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807L and overflow =
  // 0.
  if (!(smull_max_1_res == smull_max))
    goto ERROR;

  if (!(smull_max_1_ov == 0))
    goto ERROR;


  long int smull_min_1_res;
  int smull_min_1_ov;
  smull_min_1_ov = __builtin_smull_overflow(smull_min, 1L, &smull_min_1_res);

  // -9223372036854775808L * 1L = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808L and overflow
  // = 0.
  if (!(smull_min_1_res == smull_min))
    goto ERROR;

  if (!(smull_min_1_ov == 0))
    goto ERROR;


  long int smull_max_m1_res;
  int smull_max_m1_ov;
  smull_max_m1_ov = __builtin_smull_overflow(smull_max, -1L, &smull_max_m1_res);

  // 9223372036854775807L * -1L = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807L and overflow
  // = 0.
  if (!(smull_max_m1_res == - ((long int)((~0UL) >> 1))))
    goto ERROR;

  if (!(smull_max_m1_ov == 0))
    goto ERROR;


  long int smull_min_m1_res;
  int smull_min_m1_ov;
  smull_min_m1_ov = __builtin_smull_overflow(smull_min, -1L, &smull_min_m1_res);

  // -9223372036854775808L * -1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  if (!(smull_min_m1_res == smull_min))
    goto ERROR;

  if (!(smull_min_m1_ov == 1))
    goto ERROR;


  long int smull_max_2_res;
  int smull_max_2_ov;
  smull_max_2_ov = __builtin_smull_overflow(smull_max, 2L, &smull_max_2_res);

  // 9223372036854775807L * 2L = 18446744073709551614, outside the destination range; stored result = -2L and overflow = 1.
  if (!(smull_max_2_res == -2L))
    goto ERROR;

  if (!(smull_max_2_ov == 1))
    goto ERROR;


  long int smull_min_2_res;
  int smull_min_2_ov;
  smull_min_2_ov = __builtin_smull_overflow(smull_min, 2L, &smull_min_2_res);

  // -9223372036854775808L * 2L = -18446744073709551616, outside the destination range; stored result = 0L and overflow = 1.
  if (!(smull_min_2_res == 0L))
    goto ERROR;

  if (!(smull_min_2_ov == 1))
    goto ERROR;


  long int smull_1_m1_res;
  int smull_1_m1_ov;
  smull_1_m1_ov = __builtin_smull_overflow(-1L, -1L, &smull_1_m1_res);

  // -1L * -1L = 1, which fits the destination range; stored result = 1L and overflow = 0.
  if (!(smull_1_m1_res == 1L))
    goto ERROR;

  if (!(smull_1_m1_ov == 0))
    goto ERROR;


  // Signed long long int multiplication overflow tests.

  long long int smulll_0_0_res;
  int smulll_0_0_ov;
  smulll_0_0_ov = __builtin_smulll_overflow(0LL, 0LL, &smulll_0_0_res);

  // 0LL * 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  if (!(smulll_0_0_res == 0LL))
    goto ERROR;

  if (!(smulll_0_0_ov == 0))
    goto ERROR;


  long long int smulll_max_0_res;
  int smulll_max_0_ov;
  smulll_max_0_ov = __builtin_smulll_overflow(ll_max, 0LL, &smulll_max_0_res);

  // 9223372036854775807LL * 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  if (!(smulll_max_0_res == 0LL))
    goto ERROR;

  if (!(smulll_max_0_ov == 0))
    goto ERROR;


  long long int smulll_min_0_res;
  int smulll_min_0_ov;
  smulll_min_0_ov = __builtin_smulll_overflow(ll_min, 0LL, &smulll_min_0_res);

  // -9223372036854775808LL * 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  if (!(smulll_min_0_res == 0LL))
    goto ERROR;

  if (!(smulll_min_0_ov == 0))
    goto ERROR;


  long long int smulll_max_1_res;
  int smulll_max_1_ov;
  smulll_max_1_ov = __builtin_smulll_overflow(ll_max, 1LL, &smulll_max_1_res);

  // 9223372036854775807LL * 1LL = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807LL and overflow
  // = 0.
  if (!(smulll_max_1_res == ll_max))
    goto ERROR;

  if (!(smulll_max_1_ov == 0))
    goto ERROR;


  long long int smulll_min_1_res;
  int smulll_min_1_ov;
  smulll_min_1_ov = __builtin_smulll_overflow(ll_min, 1LL, &smulll_min_1_res);

  // -9223372036854775808LL * 1LL = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808LL and
  // overflow = 0.
  if (!(smulll_min_1_res == ll_min))
    goto ERROR;

  if (!(smulll_min_1_ov == 0))
    goto ERROR;


  long long int smulll_max_m1_res;
  int smulll_max_m1_ov;
  smulll_max_m1_ov = __builtin_smulll_overflow(ll_max, -1LL, &smulll_max_m1_res);

  // 9223372036854775807LL * -1LL = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807LL and
  // overflow = 0.
  if (!(smulll_max_m1_res == -ll_max))
    goto ERROR;

  if (!(smulll_max_m1_ov == 0))
    goto ERROR;


  long long int smulll_min_m1_res;
  int smulll_min_m1_ov;
  smulll_min_m1_ov = __builtin_smulll_overflow(ll_min, -1LL, &smulll_min_m1_res);

  // -9223372036854775808LL * -1LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  if (!(smulll_min_m1_res == ll_min))
    goto ERROR;

  if (!(smulll_min_m1_ov == 1))
    goto ERROR;


  long long int smulll_max_2_res;
  int smulll_max_2_ov;
  smulll_max_2_ov = __builtin_smulll_overflow(ll_max, 2LL, &smulll_max_2_res);

  // 9223372036854775807LL * 2LL = 18446744073709551614, outside the destination range; stored result = -2LL and overflow = 1.
  if (!(smulll_max_2_res == -2LL))
    goto ERROR;

  if (!(smulll_max_2_ov == 1))
    goto ERROR;


  long long int smulll_min_2_res;
  int smulll_min_2_ov;
  smulll_min_2_ov = __builtin_smulll_overflow(ll_min, 2LL, &smulll_min_2_res);

  // -9223372036854775808LL * 2LL = -18446744073709551616, outside the destination range; stored result = 0LL and overflow = 1.
  if (!(smulll_min_2_res == 0LL))
    goto ERROR;

  if (!(smulll_min_2_ov == 1))
    goto ERROR;


  long long int smulll_1_m1_res;
  int smulll_1_m1_ov;
  smulll_1_m1_ov = __builtin_smulll_overflow(-1LL, -1LL, &smulll_1_m1_res);

  // -1LL * -1LL = 1, which fits the destination range; stored result = 1LL and overflow = 0.
  if (!(smulll_1_m1_res == 1LL))
    goto ERROR;

  if (!(smulll_1_m1_ov == 0))
    goto ERROR;


  // Unsigned int multiplication overflow tests.

  unsigned int umul_0_uint_max_res;
  int umul_0_uint_max_ov;
  umul_0_uint_max_ov = __builtin_umul_overflow(0U, uint_max, &umul_0_uint_max_res);

  // 0U * 4294967295U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(umul_0_uint_max_res == 0U))
    goto ERROR;

  if (!(umul_0_uint_max_ov == 0))
    goto ERROR;


  unsigned int umul_1_uint_max_res;
  int umul_1_uint_max_ov;
  umul_1_uint_max_ov = __builtin_umul_overflow(1U, uint_max, &umul_1_uint_max_res);

  // 1U * 4294967295U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  if (!(umul_1_uint_max_res == uint_max))
    goto ERROR;

  if (!(umul_1_uint_max_ov == 0))
    goto ERROR;


  unsigned int umul_2_2147483647u_res;
  int umul_2_2147483647u_ov;
  umul_2_2147483647u_ov = __builtin_umul_overflow(2U, 2147483647U, &umul_2_2147483647u_res);

  // 2U * 2147483647U = 4294967294, which fits the destination range; stored result = 4294967294U and overflow = 0.
  if (!(umul_2_2147483647u_res == 4294967294U))
    goto ERROR;

  if (!(umul_2_2147483647u_ov == 0))
    goto ERROR;


  unsigned int umul_2_2147483648u_res;
  int umul_2_2147483648u_ov;
  umul_2_2147483648u_ov = __builtin_umul_overflow(2U, 2147483648U, &umul_2_2147483648u_res);

  // 2U * 2147483648U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  if (!(umul_2_2147483648u_res == 0U))
    goto ERROR;

  if (!(umul_2_2147483648u_ov == 1))
    goto ERROR;


  unsigned int umul_max_max_res;
  int umul_max_max_ov;
  umul_max_max_ov = __builtin_umul_overflow(uint_max, uint_max, &umul_max_max_res);

  // 4294967295U * 4294967295U = 18446744065119617025, outside the destination range; stored result = 1U and overflow = 1.
  if (!(umul_max_max_res == 1U))
    goto ERROR;

  if (!(umul_max_max_ov == 1))
    goto ERROR;


  unsigned int umul_max_2_res;
  int umul_max_2_ov;
  umul_max_2_ov = __builtin_umul_overflow(uint_max, 2U, &umul_max_2_res);

  // 4294967295U * 2U = 8589934590, outside the destination range; stored result = 4294967294U and overflow = 1.
  if (!(umul_max_2_res == 4294967294U))
    goto ERROR;

  if (!(umul_max_2_ov == 1))
    goto ERROR;
  unsigned long int umull_0_ulong_max_res;
  int umull_0_ulong_max_ov;
  umull_0_ulong_max_ov = __builtin_umull_overflow(0UL, umull_max, &umull_0_ulong_max_res);

  // 0UL * 18446744073709551615UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  if (!(umull_0_ulong_max_res == 0UL))
    goto ERROR;

  if (!(umull_0_ulong_max_ov == 0))
    goto ERROR;


  unsigned long int umull_1_ulong_max_res;
  int umull_1_ulong_max_ov;
  umull_1_ulong_max_ov = __builtin_umull_overflow(1UL, umull_max, &umull_1_ulong_max_res);

  // 1UL * 18446744073709551615UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  if (!(umull_1_ulong_max_res == umull_max))
    goto ERROR;

  if (!(umull_1_ulong_max_ov == 0))
    goto ERROR;


  unsigned long int umull_2_halfmax_res;
  int umull_2_halfmax_ov;
  umull_2_halfmax_ov = __builtin_umull_overflow(2UL, (~0UL) / 2UL, &umull_2_halfmax_res);

  // 2UL * 9223372036854775807UL = 18446744073709551614, which fits the destination range; stored result = 18446744073709551614UL and
  // overflow = 0.
  if (!(umull_2_halfmax_res == (~0UL) - 1UL))
    goto ERROR;

  if (!(umull_2_halfmax_ov == 0))
    goto ERROR;


  unsigned long int umull_2_halfmax1_res;
  int umull_2_halfmax1_ov;
  umull_2_halfmax1_ov = __builtin_umull_overflow(2UL, (~0UL) / 2UL + 1UL, &umull_2_halfmax1_res);

  // 2UL * 9223372036854775808UL = 18446744073709551616, outside the destination range; stored result = 0UL and overflow = 1.
  if (!(umull_2_halfmax1_res == 0UL))
    goto ERROR;

  if (!(umull_2_halfmax1_ov == 1))
    goto ERROR;


  unsigned long int umull_max_max_res;
  int umull_max_max_ov;
  umull_max_max_ov = __builtin_umull_overflow(umull_max, umull_max, &umull_max_max_res);

  // 18446744073709551615UL * 18446744073709551615UL = 340282366920938463426481119284349108225, outside the destination range; stored result
  // = 1UL and overflow = 1.
  if (!(umull_max_max_res == 1UL))
    goto ERROR;

  if (!(umull_max_max_ov == 1))
    goto ERROR;


  unsigned long int umull_max_2_res;
  int umull_max_2_ov;
  umull_max_2_ov = __builtin_umull_overflow(umull_max, 2UL, &umull_max_2_res);

  // 18446744073709551615UL * 2UL = 36893488147419103230, outside the destination range; stored result = 18446744073709551614UL and overflow
  // = 1.
  if (!(umull_max_2_res == (~0UL) - 1UL))
    goto ERROR;

  if (!(umull_max_2_ov == 1))
    goto ERROR;


  // Unsigned long long int multiplication overflow tests.

  unsigned long long int umulll_0_ull_max_res;
  int umulll_0_ull_max_ov;
  umulll_0_ull_max_ov = __builtin_umulll_overflow(0ULL, ull_max, &umulll_0_ull_max_res);

  // 0ULL * 18446744073709551615ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(umulll_0_ull_max_res == 0ULL))
    goto ERROR;

  if (!(umulll_0_ull_max_ov == 0))
    goto ERROR;


  unsigned long long int umulll_1_ull_max_res;
  int umulll_1_ull_max_ov;
  umulll_1_ull_max_ov = __builtin_umulll_overflow(1ULL, ull_max, &umulll_1_ull_max_res);

  // 1ULL * 18446744073709551615ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  if (!(umulll_1_ull_max_res == ull_max))
    goto ERROR;

  if (!(umulll_1_ull_max_ov == 0))
    goto ERROR;


  unsigned long long int umulll_2_halfmax_res;
  int umulll_2_halfmax_ov;
  umulll_2_halfmax_ov = __builtin_umulll_overflow(2ULL, 9223372036854775807ULL, &umulll_2_halfmax_res);

  // 2ULL * 9223372036854775807ULL = 18446744073709551614, which fits the destination range; stored result = 18446744073709551614ULL and
  // overflow = 0.
  if (!(umulll_2_halfmax_res == 18446744073709551614ULL))
    goto ERROR;

  if (!(umulll_2_halfmax_ov == 0))
    goto ERROR;


  unsigned long long int umulll_2_halfmax1_res;
  int umulll_2_halfmax1_ov;
  umulll_2_halfmax1_ov = __builtin_umulll_overflow(2ULL, 9223372036854775808ULL, &umulll_2_halfmax1_res);

  // 2ULL * 9223372036854775808ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  if (!(umulll_2_halfmax1_res == 0ULL))
    goto ERROR;

  if (!(umulll_2_halfmax1_ov == 1))
    goto ERROR;


  unsigned long long int umulll_max_max_res;
  int umulll_max_max_ov;
  umulll_max_max_ov = __builtin_umulll_overflow(ull_max, ull_max, &umulll_max_max_res);

  // 18446744073709551615ULL * 18446744073709551615ULL = 340282366920938463426481119284349108225, outside the destination range; stored
  // result = 1ULL and overflow = 1.
  if (!(umulll_max_max_res == 1ULL))
    goto ERROR;

  if (!(umulll_max_max_ov == 1))
    goto ERROR;


  unsigned long long int umulll_max_2_res;
  int umulll_max_2_ov;
  umulll_max_2_ov = __builtin_umulll_overflow(ull_max, 2ULL, &umulll_max_2_res);

  // 18446744073709551615ULL * 2ULL = 36893488147419103230, outside the destination range; stored result = 18446744073709551614ULL and
  // overflow = 1.
  if (!(umulll_max_2_res == 18446744073709551614ULL))
    goto ERROR;

  if (!(umulll_max_2_ov == 1))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
