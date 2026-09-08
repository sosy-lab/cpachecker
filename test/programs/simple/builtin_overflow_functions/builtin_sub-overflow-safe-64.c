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
  const unsigned long int sub_overflow_unsigned_long_max = ~0UL;
  const long int sub_overflow_long_max = (long int)((~0UL) >> 1);
  const long int sub_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);
  const unsigned long int ssubl_overflow_unsigned_long_max = ~0UL;
  const long int ssubl_overflow_long_max = (long int)((~0UL) >> 1);
  const long int ssubl_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);
  const unsigned long int usubl_overflow_unsigned_long_max = ~0UL;


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: -2147483648L - 1L fits in long, stores -2147483649L, and returns 0.
  long int model_ssubl_result;
  int model_ssubl_overflow;
  model_ssubl_overflow = __builtin_ssubl_overflow((-2147483647L - 1L), 1L, &model_ssubl_result);

  if (!(model_ssubl_result != 0L && model_ssubl_overflow == 0))
    goto ERROR;

  // Generic subtraction overflow tests.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)sub_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)sub_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  if (!((sizeof(long int) == sizeof(int) && (int)sub_overflow_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)sub_overflow_long_max == (long long int)(long int)long_long_max)))
    goto ERROR;


  // ILP32: The calculated long minimum must equal int minimum -2147483648.
  // LP64: The calculated long minimum must equal long long minimum -9223372036854775808.
  if (!((sizeof(long int) == sizeof(int) && (int)sub_overflow_long_min == int_min) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)sub_overflow_long_min ==
            (long long int)(long int)long_long_min)))
    goto ERROR;


  signed char sub_overflow_signed_char_min_minus_one_as_signed_char_result;
  int sub_overflow_signed_char_min_minus_one_as_signed_char_overflow;
  sub_overflow_signed_char_min_minus_one_as_signed_char_overflow = __builtin_sub_overflow(signed_char_min, 1, &sub_overflow_signed_char_min_minus_one_as_signed_char_result);

  // -128 - 1 = -129, outside the destination range; stored result = 127 and overflow = 1.
  if (!(sub_overflow_signed_char_min_minus_one_as_signed_char_result == signed_char_max))
    goto ERROR;

  if (!(sub_overflow_signed_char_min_minus_one_as_signed_char_overflow == 1))
    goto ERROR;


  unsigned char sub_overflow_zero_minus_one_as_unsigned_char_result;
  int sub_overflow_zero_minus_one_as_unsigned_char_overflow;
  sub_overflow_zero_minus_one_as_unsigned_char_overflow = __builtin_sub_overflow(0, 1, &sub_overflow_zero_minus_one_as_unsigned_char_result);

  // 0 - 1 = -1, outside the destination range; stored result = 255U and overflow = 1.
  if (!(sub_overflow_zero_minus_one_as_unsigned_char_result == unsigned_char_max))
    goto ERROR;

  if (!(sub_overflow_zero_minus_one_as_unsigned_char_overflow == 1))
    goto ERROR;


  short int sub_overflow_short_max_minus_minus_one_as_short_int_result;
  int sub_overflow_short_max_minus_minus_one_as_short_int_overflow;
  sub_overflow_short_max_minus_minus_one_as_short_int_overflow = __builtin_sub_overflow(short_max, -1, &sub_overflow_short_max_minus_minus_one_as_short_int_result);

  // 32767 - -1 = 32768, outside the destination range; stored result = -32768 and overflow = 1.
  if (!(sub_overflow_short_max_minus_minus_one_as_short_int_result == short_min))
    goto ERROR;

  if (!(sub_overflow_short_max_minus_minus_one_as_short_int_overflow == 1))
    goto ERROR;


  unsigned short int sub_overflow_unsigned_short_max_minus_unsigned_short_max_as_unsigned_short_int_result;
  int sub_overflow_unsigned_short_max_minus_unsigned_short_max_as_unsigned_short_int_overflow;
  sub_overflow_unsigned_short_max_minus_unsigned_short_max_as_unsigned_short_int_overflow = __builtin_sub_overflow(unsigned_short_max, unsigned_short_max, &sub_overflow_unsigned_short_max_minus_unsigned_short_max_as_unsigned_short_int_result);

  // 65535U - 65535U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(sub_overflow_unsigned_short_max_minus_unsigned_short_max_as_unsigned_short_int_result == 0U))
    goto ERROR;

  if (!(sub_overflow_unsigned_short_max_minus_unsigned_short_max_as_unsigned_short_int_overflow == 0))
    goto ERROR;


  int sub_overflow_int_min_minus_one_as_int_result;
  int sub_overflow_int_min_minus_one_as_int_overflow;
  sub_overflow_int_min_minus_one_as_int_overflow = __builtin_sub_overflow(int_min, 1, &sub_overflow_int_min_minus_one_as_int_result);

  // -2147483648 - 1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  if (!(sub_overflow_int_min_minus_one_as_int_result == int_max))
    goto ERROR;

  if (!(sub_overflow_int_min_minus_one_as_int_overflow == 1))
    goto ERROR;


  unsigned int sub_overflow_zero_minus_unsigned_int_max_as_unsigned_int_result;
  int sub_overflow_zero_minus_unsigned_int_max_as_unsigned_int_overflow;
  sub_overflow_zero_minus_unsigned_int_max_as_unsigned_int_overflow = __builtin_sub_overflow(0U, unsigned_int_max, &sub_overflow_zero_minus_unsigned_int_max_as_unsigned_int_result);

  // 0U - 4294967295U = -4294967295, outside the destination range; stored result = 1U and overflow = 1.
  if (!(sub_overflow_zero_minus_unsigned_int_max_as_unsigned_int_result == 1U))
    goto ERROR;

  if (!(sub_overflow_zero_minus_unsigned_int_max_as_unsigned_int_overflow == 1))
    goto ERROR;


  long int sub_overflow_long_max_minus_minus_one_as_long_int_result;
  int sub_overflow_long_max_minus_minus_one_as_long_int_overflow;
  sub_overflow_long_max_minus_minus_one_as_long_int_overflow = __builtin_sub_overflow(sub_overflow_long_max, -1L, &sub_overflow_long_max_minus_minus_one_as_long_int_result);

  // 9223372036854775807L - -1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  if (!(sub_overflow_long_max_minus_minus_one_as_long_int_result == sub_overflow_long_min))
    goto ERROR;

  if (!(sub_overflow_long_max_minus_minus_one_as_long_int_overflow == 1))
    goto ERROR;


  unsigned long int sub_overflow_zero_minus_one_as_unsigned_long_int_result;
  int sub_overflow_zero_minus_one_as_unsigned_long_int_overflow;
  sub_overflow_zero_minus_one_as_unsigned_long_int_overflow = __builtin_sub_overflow(0UL, 1UL, &sub_overflow_zero_minus_one_as_unsigned_long_int_result);

  // 0UL - 1UL = -1, outside the destination range; stored result = 18446744073709551615UL and overflow = 1.
  if (!(sub_overflow_zero_minus_one_as_unsigned_long_int_result == sub_overflow_unsigned_long_max))
    goto ERROR;

  if (!(sub_overflow_zero_minus_one_as_unsigned_long_int_overflow == 1))
    goto ERROR;


  long long int sub_overflow_zero_minus_long_long_min_as_long_long_int_result;
  int sub_overflow_zero_minus_long_long_min_as_long_long_int_overflow;
  sub_overflow_zero_minus_long_long_min_as_long_long_int_overflow = __builtin_sub_overflow(0LL, long_long_min, &sub_overflow_zero_minus_long_long_min_as_long_long_int_result);

  // 0LL - -9223372036854775808LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  if (!(sub_overflow_zero_minus_long_long_min_as_long_long_int_result == long_long_min))
    goto ERROR;

  if (!(sub_overflow_zero_minus_long_long_min_as_long_long_int_overflow == 1))
    goto ERROR;


  unsigned long long int sub_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_as_unsigned_long_long_int_result;
  int sub_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_as_unsigned_long_long_int_overflow;
  sub_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_as_unsigned_long_long_int_overflow = __builtin_sub_overflow(unsigned_long_long_max, unsigned_long_long_max, &sub_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_as_unsigned_long_long_int_result);

  // 18446744073709551615ULL - 18446744073709551615ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(sub_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_as_unsigned_long_long_int_result == 0ULL))
    goto ERROR;

  if (!(sub_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_as_unsigned_long_long_int_overflow == 0))
    goto ERROR;


  char sub_overflow_char_min_minus_one_as_char_result;
  int sub_overflow_char_min_minus_one_as_char_overflow;
  sub_overflow_char_min_minus_one_as_char_overflow = __builtin_sub_overflow(char_min, 1, &sub_overflow_char_min_minus_one_as_char_result);

  // (char)-128 - 1 = -129, outside the destination range; stored result = (char)127 and overflow = 1.
  if (!(sub_overflow_char_min_minus_one_as_char_result == char_max))
    goto ERROR;

  if (!(sub_overflow_char_min_minus_one_as_char_overflow == 1))
    goto ERROR;


  int sub_overflow_one_minus_one_as_int_result;
  int sub_overflow_one_minus_one_as_int_overflow;
  sub_overflow_one_minus_one_as_int_overflow = __builtin_sub_overflow(1ULL, 1LL, &sub_overflow_one_minus_one_as_int_result);

  // 1ULL - 1LL = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(sub_overflow_one_minus_one_as_int_result == 0))
    goto ERROR;

  if (!(sub_overflow_one_minus_one_as_int_overflow == 0))
    goto ERROR;


  unsigned int sub_overflow_minus_one_minus_zero_as_unsigned_int_result;
  int sub_overflow_minus_one_minus_zero_as_unsigned_int_overflow;
  sub_overflow_minus_one_minus_zero_as_unsigned_int_overflow = __builtin_sub_overflow(-1LL, 0ULL, &sub_overflow_minus_one_minus_zero_as_unsigned_int_result);

  // -1LL - 0ULL = -1, outside the destination range; stored result = 4294967295U and overflow = 1.
  if (!(sub_overflow_minus_one_minus_zero_as_unsigned_int_result == unsigned_int_max))
    goto ERROR;

  if (!(sub_overflow_minus_one_minus_zero_as_unsigned_int_overflow == 1))
    goto ERROR;


  // Signed int subtraction overflow tests.

  int ssub_overflow_zero_minus_zero_result;
  int ssub_overflow_zero_minus_zero_overflow;
  ssub_overflow_zero_minus_zero_overflow = __builtin_ssub_overflow(0, 0, &ssub_overflow_zero_minus_zero_result);

  // 0 - 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(ssub_overflow_zero_minus_zero_result == 0))
    goto ERROR;

  if (!(ssub_overflow_zero_minus_zero_overflow == 0))
    goto ERROR;


  int ssub_overflow_int_max_minus_zero_result;
  int ssub_overflow_int_max_minus_zero_overflow;
  ssub_overflow_int_max_minus_zero_overflow = __builtin_ssub_overflow(int_max, 0, &ssub_overflow_int_max_minus_zero_result);

  // 2147483647 - 0 = 2147483647, which fits the destination range; stored result = 2147483647 and overflow = 0.
  if (!(ssub_overflow_int_max_minus_zero_result == int_max))
    goto ERROR;

  if (!(ssub_overflow_int_max_minus_zero_overflow == 0))
    goto ERROR;


  int ssub_overflow_int_min_minus_zero_result;
  int ssub_overflow_int_min_minus_zero_overflow;
  ssub_overflow_int_min_minus_zero_overflow = __builtin_ssub_overflow(int_min, 0, &ssub_overflow_int_min_minus_zero_result);

  // -2147483648 - 0 = -2147483648, which fits the destination range; stored result = -2147483648 and overflow = 0.
  if (!(ssub_overflow_int_min_minus_zero_result == int_min))
    goto ERROR;

  if (!(ssub_overflow_int_min_minus_zero_overflow == 0))
    goto ERROR;


  int ssub_overflow_int_min_minus_minus_one_result;
  int ssub_overflow_int_min_minus_minus_one_overflow;
  ssub_overflow_int_min_minus_minus_one_overflow = __builtin_ssub_overflow(int_min, -1, &ssub_overflow_int_min_minus_minus_one_result);

  // -2147483648 - -1 = -2147483647, which fits the destination range; stored result = -2147483647 and overflow = 0.
  if (!(ssub_overflow_int_min_minus_minus_one_result == -int_max))
    goto ERROR;

  if (!(ssub_overflow_int_min_minus_minus_one_overflow == 0))
    goto ERROR;


  int ssub_overflow_int_max_minus_one_result;
  int ssub_overflow_int_max_minus_one_overflow;
  ssub_overflow_int_max_minus_one_overflow = __builtin_ssub_overflow(int_max, 1, &ssub_overflow_int_max_minus_one_result);

  // 2147483647 - 1 = 2147483646, which fits the destination range; stored result = 2147483646 and overflow = 0.
  if (!(ssub_overflow_int_max_minus_one_result == 2147483646))
    goto ERROR;

  if (!(ssub_overflow_int_max_minus_one_overflow == 0))
    goto ERROR;


  int ssub_overflow_int_max_minus_minus_one_result;
  int ssub_overflow_int_max_minus_minus_one_overflow;
  ssub_overflow_int_max_minus_minus_one_overflow = __builtin_ssub_overflow(int_max, -1, &ssub_overflow_int_max_minus_minus_one_result);

  // 2147483647 - -1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(ssub_overflow_int_max_minus_minus_one_result == int_min))
    goto ERROR;

  if (!(ssub_overflow_int_max_minus_minus_one_overflow == 1))
    goto ERROR;


  int ssub_overflow_int_min_minus_one_result;
  int ssub_overflow_int_min_minus_one_overflow;
  ssub_overflow_int_min_minus_one_overflow = __builtin_ssub_overflow(int_min, 1, &ssub_overflow_int_min_minus_one_result);

  // -2147483648 - 1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  if (!(ssub_overflow_int_min_minus_one_result == int_max))
    goto ERROR;

  if (!(ssub_overflow_int_min_minus_one_overflow == 1))
    goto ERROR;


  int ssub_overflow_zero_minus_int_min_result;
  int ssub_overflow_zero_minus_int_min_overflow;
  ssub_overflow_zero_minus_int_min_overflow = __builtin_ssub_overflow(0, int_min, &ssub_overflow_zero_minus_int_min_result);

  // 0 - -2147483648 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(ssub_overflow_zero_minus_int_min_result == int_min))
    goto ERROR;

  if (!(ssub_overflow_zero_minus_int_min_overflow == 1))
    goto ERROR;


  // Signed long int subtraction overflow tests.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)ssubl_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)ssubl_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  if (!((sizeof(long int) == sizeof(int) && (int)ssubl_overflow_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)ssubl_overflow_long_max == (long long int)(long int)long_long_max)))
    goto ERROR;


  // ILP32: The calculated long minimum must equal int minimum -2147483648.
  // LP64: The calculated long minimum must equal long long minimum -9223372036854775808.
  if (!((sizeof(long int) == sizeof(int) && (int)ssubl_overflow_long_min == int_min) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)ssubl_overflow_long_min ==
            (long long int)(long int)long_long_min)))
    goto ERROR;


  long int ssubl_overflow_zero_minus_zero_result;
  int ssubl_overflow_zero_minus_zero_overflow;
  ssubl_overflow_zero_minus_zero_overflow = __builtin_ssubl_overflow(0L, 0L, &ssubl_overflow_zero_minus_zero_result);

  // 0L - 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  if (!(ssubl_overflow_zero_minus_zero_result == 0L))
    goto ERROR;

  if (!(ssubl_overflow_zero_minus_zero_overflow == 0))
    goto ERROR;


  long int ssubl_overflow_long_max_minus_zero_result;
  int ssubl_overflow_long_max_minus_zero_overflow;
  ssubl_overflow_long_max_minus_zero_overflow = __builtin_ssubl_overflow(ssubl_overflow_long_max, 0L, &ssubl_overflow_long_max_minus_zero_result);

  // 9223372036854775807L - 0L = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807L and overflow =
  // 0.
  if (!(ssubl_overflow_long_max_minus_zero_result == ssubl_overflow_long_max))
    goto ERROR;

  if (!(ssubl_overflow_long_max_minus_zero_overflow == 0))
    goto ERROR;


  long int ssubl_overflow_long_min_minus_zero_result;
  int ssubl_overflow_long_min_minus_zero_overflow;
  ssubl_overflow_long_min_minus_zero_overflow = __builtin_ssubl_overflow(ssubl_overflow_long_min, 0L, &ssubl_overflow_long_min_minus_zero_result);

  // -9223372036854775808L - 0L = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808L and overflow
  // = 0.
  if (!(ssubl_overflow_long_min_minus_zero_result == ssubl_overflow_long_min))
    goto ERROR;

  if (!(ssubl_overflow_long_min_minus_zero_overflow == 0))
    goto ERROR;


  long int ssubl_overflow_long_min_minus_minus_one_result;
  int ssubl_overflow_long_min_minus_minus_one_overflow;
  ssubl_overflow_long_min_minus_minus_one_overflow = __builtin_ssubl_overflow(ssubl_overflow_long_min, -1L, &ssubl_overflow_long_min_minus_minus_one_result);

  // -9223372036854775808L - -1L = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807L and
  // overflow = 0.
  if (!(ssubl_overflow_long_min_minus_minus_one_result == ((-((long int)((~0UL) >> 1)) - 1L)) + 1L))
    goto ERROR;

  if (!(ssubl_overflow_long_min_minus_minus_one_overflow == 0))
    goto ERROR;


  long int ssubl_overflow_long_max_minus_one_result;
  int ssubl_overflow_long_max_minus_one_overflow;
  ssubl_overflow_long_max_minus_one_overflow = __builtin_ssubl_overflow(ssubl_overflow_long_max, 1L, &ssubl_overflow_long_max_minus_one_result);

  // ssubl_overflow_long_max - 1L = ssubl_overflow_long_max - 1L, which fits the destination range; stored result = 9223372036854775806L and
  // overflow = 0.
  if (!(ssubl_overflow_long_max_minus_one_result == ((long int)((~0UL) >> 1)) - 1L))
    goto ERROR;

  if (!(ssubl_overflow_long_max_minus_one_overflow == 0))
    goto ERROR;


  long int ssubl_overflow_long_max_minus_minus_one_result;
  int ssubl_overflow_long_max_minus_minus_one_overflow;
  ssubl_overflow_long_max_minus_minus_one_overflow = __builtin_ssubl_overflow(ssubl_overflow_long_max, -1L, &ssubl_overflow_long_max_minus_minus_one_result);

  // 9223372036854775807L - -1L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  if (!(ssubl_overflow_long_max_minus_minus_one_result == ssubl_overflow_long_min))
    goto ERROR;

  if (!(ssubl_overflow_long_max_minus_minus_one_overflow == 1))
    goto ERROR;


  long int ssubl_overflow_long_min_minus_one_result;
  int ssubl_overflow_long_min_minus_one_overflow;
  ssubl_overflow_long_min_minus_one_overflow = __builtin_ssubl_overflow(ssubl_overflow_long_min, 1L, &ssubl_overflow_long_min_minus_one_result);

  // -9223372036854775808L - 1L = -9223372036854775809, outside the destination range; stored result = 9223372036854775807L and overflow =
  // 1.
  if (!(ssubl_overflow_long_min_minus_one_result == ssubl_overflow_long_max))
    goto ERROR;

  if (!(ssubl_overflow_long_min_minus_one_overflow == 1))
    goto ERROR;


  long int ssubl_overflow_zero_minus_long_min_result;
  int ssubl_overflow_zero_minus_long_min_overflow;
  ssubl_overflow_zero_minus_long_min_overflow = __builtin_ssubl_overflow(0L, ssubl_overflow_long_min, &ssubl_overflow_zero_minus_long_min_result);

  // 0L - -9223372036854775808L = 9223372036854775808, outside the destination range; stored result = -9223372036854775808L and overflow =
  // 1.
  if (!(ssubl_overflow_zero_minus_long_min_result == ssubl_overflow_long_min))
    goto ERROR;

  if (!(ssubl_overflow_zero_minus_long_min_overflow == 1))
    goto ERROR;


  // Signed long long int subtraction overflow tests.

  long long int ssubll_overflow_zero_minus_zero_result;
  int ssubll_overflow_zero_minus_zero_overflow;
  ssubll_overflow_zero_minus_zero_overflow = __builtin_ssubll_overflow(0LL, 0LL, &ssubll_overflow_zero_minus_zero_result);

  // 0LL - 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  if (!(ssubll_overflow_zero_minus_zero_result == 0LL))
    goto ERROR;

  if (!(ssubll_overflow_zero_minus_zero_overflow == 0))
    goto ERROR;


  long long int ssubll_overflow_long_long_max_minus_zero_result;
  int ssubll_overflow_long_long_max_minus_zero_overflow;
  ssubll_overflow_long_long_max_minus_zero_overflow = __builtin_ssubll_overflow(long_long_max, 0LL, &ssubll_overflow_long_long_max_minus_zero_result);

  // 9223372036854775807LL - 0LL = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807LL and overflow
  // = 0.
  if (!(ssubll_overflow_long_long_max_minus_zero_result == long_long_max))
    goto ERROR;

  if (!(ssubll_overflow_long_long_max_minus_zero_overflow == 0))
    goto ERROR;


  long long int ssubll_overflow_long_long_min_minus_zero_result;
  int ssubll_overflow_long_long_min_minus_zero_overflow;
  ssubll_overflow_long_long_min_minus_zero_overflow = __builtin_ssubll_overflow(long_long_min, 0LL, &ssubll_overflow_long_long_min_minus_zero_result);

  // -9223372036854775808LL - 0LL = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808LL and
  // overflow = 0.
  if (!(ssubll_overflow_long_long_min_minus_zero_result == long_long_min))
    goto ERROR;

  if (!(ssubll_overflow_long_long_min_minus_zero_overflow == 0))
    goto ERROR;


  long long int ssubll_overflow_long_long_min_minus_minus_one_result;
  int ssubll_overflow_long_long_min_minus_minus_one_overflow;
  ssubll_overflow_long_long_min_minus_minus_one_overflow = __builtin_ssubll_overflow(long_long_min, -1LL, &ssubll_overflow_long_long_min_minus_minus_one_result);

  // -9223372036854775808LL - -1LL = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807LL and
  // overflow = 0.
  if (!(ssubll_overflow_long_long_min_minus_minus_one_result == -long_long_max))
    goto ERROR;

  if (!(ssubll_overflow_long_long_min_minus_minus_one_overflow == 0))
    goto ERROR;


  long long int ssubll_overflow_long_long_max_minus_one_result;
  int ssubll_overflow_long_long_max_minus_one_overflow;
  ssubll_overflow_long_long_max_minus_one_overflow = __builtin_ssubll_overflow(long_long_max, 1LL, &ssubll_overflow_long_long_max_minus_one_result);

  // 9223372036854775807LL - 1LL = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806LL and overflow
  // = 0.
  if (!(ssubll_overflow_long_long_max_minus_one_result == 9223372036854775806LL))
    goto ERROR;

  if (!(ssubll_overflow_long_long_max_minus_one_overflow == 0))
    goto ERROR;


  long long int ssubll_overflow_long_long_max_minus_minus_one_result;
  int ssubll_overflow_long_long_max_minus_minus_one_overflow;
  ssubll_overflow_long_long_max_minus_minus_one_overflow = __builtin_ssubll_overflow(long_long_max, -1LL, &ssubll_overflow_long_long_max_minus_minus_one_result);

  // 9223372036854775807LL - -1LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  if (!(ssubll_overflow_long_long_max_minus_minus_one_result == long_long_min))
    goto ERROR;

  if (!(ssubll_overflow_long_long_max_minus_minus_one_overflow == 1))
    goto ERROR;


  long long int ssubll_overflow_long_long_min_minus_one_result;
  int ssubll_overflow_long_long_min_minus_one_overflow;
  ssubll_overflow_long_long_min_minus_one_overflow = __builtin_ssubll_overflow(long_long_min, 1LL, &ssubll_overflow_long_long_min_minus_one_result);

  // -9223372036854775808LL - 1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  if (!(ssubll_overflow_long_long_min_minus_one_result == long_long_max))
    goto ERROR;

  if (!(ssubll_overflow_long_long_min_minus_one_overflow == 1))
    goto ERROR;


  long long int ssubll_overflow_zero_minus_long_long_min_result;
  int ssubll_overflow_zero_minus_long_long_min_overflow;
  ssubll_overflow_zero_minus_long_long_min_overflow = __builtin_ssubll_overflow(0LL, long_long_min, &ssubll_overflow_zero_minus_long_long_min_result);

  // 0LL - -9223372036854775808LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow
  // = 1.
  if (!(ssubll_overflow_zero_minus_long_long_min_result == long_long_min))
    goto ERROR;

  if (!(ssubll_overflow_zero_minus_long_long_min_overflow == 1))
    goto ERROR;


  // Unsigned int subtraction overflow tests.

  unsigned int usub_overflow_zero_minus_zero_result;
  int usub_overflow_zero_minus_zero_overflow;
  usub_overflow_zero_minus_zero_overflow = __builtin_usub_overflow(0U, 0U, &usub_overflow_zero_minus_zero_result);

  // 0U - 0U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(usub_overflow_zero_minus_zero_result == 0U))
    goto ERROR;

  if (!(usub_overflow_zero_minus_zero_overflow == 0))
    goto ERROR;


  unsigned int usub_overflow_unsigned_int_max_minus_zero_result;
  int usub_overflow_unsigned_int_max_minus_zero_overflow;
  usub_overflow_unsigned_int_max_minus_zero_overflow = __builtin_usub_overflow(unsigned_int_max, 0U, &usub_overflow_unsigned_int_max_minus_zero_result);

  // 4294967295U - 0U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  if (!(usub_overflow_unsigned_int_max_minus_zero_result == unsigned_int_max))
    goto ERROR;

  if (!(usub_overflow_unsigned_int_max_minus_zero_overflow == 0))
    goto ERROR;


  unsigned int usub_overflow_unsigned_int_max_minus_unsigned_int_max_result;
  int usub_overflow_unsigned_int_max_minus_unsigned_int_max_overflow;
  usub_overflow_unsigned_int_max_minus_unsigned_int_max_overflow = __builtin_usub_overflow(unsigned_int_max, unsigned_int_max, &usub_overflow_unsigned_int_max_minus_unsigned_int_max_result);

  // 4294967295U - 4294967295U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(usub_overflow_unsigned_int_max_minus_unsigned_int_max_result == 0U))
    goto ERROR;

  if (!(usub_overflow_unsigned_int_max_minus_unsigned_int_max_overflow == 0))
    goto ERROR;


  unsigned int usub_overflow_one_minus_one_result;
  int usub_overflow_one_minus_one_overflow;
  usub_overflow_one_minus_one_overflow = __builtin_usub_overflow(1U, 1U, &usub_overflow_one_minus_one_result);

  // 1U - 1U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(usub_overflow_one_minus_one_result == 0U))
    goto ERROR;

  if (!(usub_overflow_one_minus_one_overflow == 0))
    goto ERROR;


  unsigned int usub_overflow_zero_minus_one_result;
  int usub_overflow_zero_minus_one_overflow;
  usub_overflow_zero_minus_one_overflow = __builtin_usub_overflow(0U, 1U, &usub_overflow_zero_minus_one_result);

  // 0U - 1U = -1, outside the destination range; stored result = 4294967295U and overflow = 1.
  if (!(usub_overflow_zero_minus_one_result == unsigned_int_max))
    goto ERROR;

  if (!(usub_overflow_zero_minus_one_overflow == 1))
    goto ERROR;


  unsigned int usub_overflow_zero_minus_unsigned_int_max_result;
  int usub_overflow_zero_minus_unsigned_int_max_overflow;
  usub_overflow_zero_minus_unsigned_int_max_overflow = __builtin_usub_overflow(0U, unsigned_int_max, &usub_overflow_zero_minus_unsigned_int_max_result);

  // 0U - 4294967295U = -4294967295, outside the destination range; stored result = 1U and overflow = 1.
  if (!(usub_overflow_zero_minus_unsigned_int_max_result == 1U))
    goto ERROR;

  if (!(usub_overflow_zero_minus_unsigned_int_max_overflow == 1))
    goto ERROR;


  unsigned int usub_overflow_one_minus_two_result;
  int usub_overflow_one_minus_two_overflow;
  usub_overflow_one_minus_two_overflow = __builtin_usub_overflow(1U, 2U, &usub_overflow_one_minus_two_result);

  // 1U - 2U = -1, outside the destination range; stored result = 4294967295U and overflow = 1.
  if (!(usub_overflow_one_minus_two_result == unsigned_int_max))
    goto ERROR;

  if (!(usub_overflow_one_minus_two_overflow == 1))
    goto ERROR;


  // Unsigned long int subtraction overflow tests.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)usubl_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)usubl_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  unsigned long int usubl_overflow_zero_minus_zero_result;
  int usubl_overflow_zero_minus_zero_overflow;
  usubl_overflow_zero_minus_zero_overflow = __builtin_usubl_overflow(0UL, 0UL, &usubl_overflow_zero_minus_zero_result);

  // 0UL - 0UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  if (!(usubl_overflow_zero_minus_zero_result == 0UL))
    goto ERROR;

  if (!(usubl_overflow_zero_minus_zero_overflow == 0))
    goto ERROR;


  unsigned long int usubl_overflow_unsigned_long_max_minus_zero_result;
  int usubl_overflow_unsigned_long_max_minus_zero_overflow;
  usubl_overflow_unsigned_long_max_minus_zero_overflow = __builtin_usubl_overflow(usubl_overflow_unsigned_long_max, 0UL, &usubl_overflow_unsigned_long_max_minus_zero_result);

  // 18446744073709551615UL - 0UL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615UL and
  // overflow = 0.
  if (!(usubl_overflow_unsigned_long_max_minus_zero_result == usubl_overflow_unsigned_long_max))
    goto ERROR;

  if (!(usubl_overflow_unsigned_long_max_minus_zero_overflow == 0))
    goto ERROR;


  unsigned long int usubl_overflow_unsigned_long_max_minus_unsigned_long_max_result;
  int usubl_overflow_unsigned_long_max_minus_unsigned_long_max_overflow;
  usubl_overflow_unsigned_long_max_minus_unsigned_long_max_overflow = __builtin_usubl_overflow(usubl_overflow_unsigned_long_max, usubl_overflow_unsigned_long_max, &usubl_overflow_unsigned_long_max_minus_unsigned_long_max_result);

  // 18446744073709551615UL - 18446744073709551615UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  if (!(usubl_overflow_unsigned_long_max_minus_unsigned_long_max_result == 0UL))
    goto ERROR;

  if (!(usubl_overflow_unsigned_long_max_minus_unsigned_long_max_overflow == 0))
    goto ERROR;


  unsigned long int usubl_overflow_one_minus_one_result;
  int usubl_overflow_one_minus_one_overflow;
  usubl_overflow_one_minus_one_overflow = __builtin_usubl_overflow(1UL, 1UL, &usubl_overflow_one_minus_one_result);

  // 1UL - 1UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  if (!(usubl_overflow_one_minus_one_result == 0UL))
    goto ERROR;

  if (!(usubl_overflow_one_minus_one_overflow == 0))
    goto ERROR;


  unsigned long int usubl_overflow_zero_minus_one_result;
  int usubl_overflow_zero_minus_one_overflow;
  usubl_overflow_zero_minus_one_overflow = __builtin_usubl_overflow(0UL, 1UL, &usubl_overflow_zero_minus_one_result);

  // 0UL - 1UL = -1, outside the destination range; stored result = 18446744073709551615UL and overflow = 1.
  if (!(usubl_overflow_zero_minus_one_result == usubl_overflow_unsigned_long_max))
    goto ERROR;

  if (!(usubl_overflow_zero_minus_one_overflow == 1))
    goto ERROR;


  unsigned long int usubl_overflow_zero_minus_unsigned_long_max_result;
  int usubl_overflow_zero_minus_unsigned_long_max_overflow;
  usubl_overflow_zero_minus_unsigned_long_max_overflow = __builtin_usubl_overflow(0UL, usubl_overflow_unsigned_long_max, &usubl_overflow_zero_minus_unsigned_long_max_result);

  // 0UL - 18446744073709551615UL = -18446744073709551615, outside the destination range; stored result = 1UL and overflow = 1.
  if (!(usubl_overflow_zero_minus_unsigned_long_max_result == 1UL))
    goto ERROR;

  if (!(usubl_overflow_zero_minus_unsigned_long_max_overflow == 1))
    goto ERROR;


  unsigned long int usubl_overflow_one_minus_two_result;
  int usubl_overflow_one_minus_two_overflow;
  usubl_overflow_one_minus_two_overflow = __builtin_usubl_overflow(1UL, 2UL, &usubl_overflow_one_minus_two_result);

  // 1UL - 2UL = -1, outside the destination range; stored result = 18446744073709551615UL and overflow = 1.
  if (!(usubl_overflow_one_minus_two_result == usubl_overflow_unsigned_long_max))
    goto ERROR;

  if (!(usubl_overflow_one_minus_two_overflow == 1))
    goto ERROR;


  // Unsigned long long int subtraction overflow tests.

  unsigned long long int usubll_overflow_zero_minus_zero_result;
  int usubll_overflow_zero_minus_zero_overflow;
  usubll_overflow_zero_minus_zero_overflow = __builtin_usubll_overflow(0ULL, 0ULL, &usubll_overflow_zero_minus_zero_result);

  // 0ULL - 0ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(usubll_overflow_zero_minus_zero_result == 0ULL))
    goto ERROR;

  if (!(usubll_overflow_zero_minus_zero_overflow == 0))
    goto ERROR;


  unsigned long long int usubll_overflow_unsigned_long_long_max_minus_zero_result;
  int usubll_overflow_unsigned_long_long_max_minus_zero_overflow;
  usubll_overflow_unsigned_long_long_max_minus_zero_overflow = __builtin_usubll_overflow(unsigned_long_long_max, 0ULL, &usubll_overflow_unsigned_long_long_max_minus_zero_result);

  // 18446744073709551615ULL - 0ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  if (!(usubll_overflow_unsigned_long_long_max_minus_zero_result == unsigned_long_long_max))
    goto ERROR;

  if (!(usubll_overflow_unsigned_long_long_max_minus_zero_overflow == 0))
    goto ERROR;


  unsigned long long int usubll_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_result;
  int usubll_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_overflow;
  usubll_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_overflow = __builtin_usubll_overflow(unsigned_long_long_max, unsigned_long_long_max, &usubll_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_result);

  // 18446744073709551615ULL - 18446744073709551615ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(usubll_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_result == 0ULL))
    goto ERROR;

  if (!(usubll_overflow_unsigned_long_long_max_minus_unsigned_long_long_max_overflow == 0))
    goto ERROR;


  unsigned long long int usubll_overflow_one_minus_one_result;
  int usubll_overflow_one_minus_one_overflow;
  usubll_overflow_one_minus_one_overflow = __builtin_usubll_overflow(1ULL, 1ULL, &usubll_overflow_one_minus_one_result);

  // 1ULL - 1ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(usubll_overflow_one_minus_one_result == 0ULL))
    goto ERROR;

  if (!(usubll_overflow_one_minus_one_overflow == 0))
    goto ERROR;


  unsigned long long int usubll_overflow_zero_minus_one_result;
  int usubll_overflow_zero_minus_one_overflow;
  usubll_overflow_zero_minus_one_overflow = __builtin_usubll_overflow(0ULL, 1ULL, &usubll_overflow_zero_minus_one_result);

  // 0ULL - 1ULL = -1, outside the destination range; stored result = 18446744073709551615ULL and overflow = 1.
  if (!(usubll_overflow_zero_minus_one_result == unsigned_long_long_max))
    goto ERROR;

  if (!(usubll_overflow_zero_minus_one_overflow == 1))
    goto ERROR;


  unsigned long long int usubll_overflow_zero_minus_unsigned_long_long_max_result;
  int usubll_overflow_zero_minus_unsigned_long_long_max_overflow;
  usubll_overflow_zero_minus_unsigned_long_long_max_overflow = __builtin_usubll_overflow(0ULL, unsigned_long_long_max, &usubll_overflow_zero_minus_unsigned_long_long_max_result);

  // 0ULL - 18446744073709551615ULL = -18446744073709551615, outside the destination range; stored result = 1ULL and overflow = 1.
  if (!(usubll_overflow_zero_minus_unsigned_long_long_max_result == 1ULL))
    goto ERROR;

  if (!(usubll_overflow_zero_minus_unsigned_long_long_max_overflow == 1))
    goto ERROR;


  unsigned long long int usubll_overflow_one_minus_two_result;
  int usubll_overflow_one_minus_two_overflow;
  usubll_overflow_one_minus_two_overflow = __builtin_usubll_overflow(1ULL, 2ULL, &usubll_overflow_one_minus_two_result);

  // 1ULL - 2ULL = -1, outside the destination range; stored result = 18446744073709551615ULL and overflow = 1.
  if (!(usubll_overflow_one_minus_two_result == unsigned_long_long_max))
    goto ERROR;

  if (!(usubll_overflow_one_minus_two_overflow == 1))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
