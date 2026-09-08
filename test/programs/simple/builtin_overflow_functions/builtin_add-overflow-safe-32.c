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
  const int int_min = (-2147483647 - 1);
  const int int_max = 2147483647;
  const unsigned int unsigned_int_max = 4294967295U;
  const long long int long_long_min = (-9223372036854775807LL - 1LL);
  const long long int long_long_max = 9223372036854775807LL;
  const unsigned long long int unsigned_long_long_max = 18446744073709551615ULL;
  const unsigned long int add_overflow_unsigned_long_max = ~0UL;
  const long int add_overflow_long_max = (long int)((~0UL) >> 1);
  const long int add_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);
  const unsigned long int saddl_overflow_unsigned_long_max = ~0UL;
  const long int saddl_overflow_long_max = (long int)((~0UL) >> 1);
  const long int saddl_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);
  const unsigned long int uaddl_overflow_unsigned_long_max = ~0UL;


  // This program targets ILP32 and fails its expected verdict under LP64.

  // ILP32: 2147483647L + 1L = 2147483648, outside long range; stored result = -2147483648L and overflow = 1.
  long int model_saddl_result;
  int model_saddl_overflow;
  model_saddl_overflow = __builtin_saddl_overflow(2147483647L, 1L, &model_saddl_result);

  if (!(model_saddl_result != 0L && model_saddl_overflow == 1))
    goto ERROR;

  // Generic addition overflow tests.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)add_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)add_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  if (!((sizeof(long int) == sizeof(int) && (int)add_overflow_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)add_overflow_long_max == (long long int)(long int)long_long_max)))
    goto ERROR;


  // ILP32: The calculated long minimum must equal int minimum -2147483648.
  // LP64: The calculated long minimum must equal long long minimum -9223372036854775808.
  if (!((sizeof(long int) == sizeof(int) && (int)add_overflow_long_min == int_min) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)add_overflow_long_min ==
            (long long int)(long int)long_long_min)))
    goto ERROR;


  signed char add_overflow_signed_char_max_plus_zero_as_signed_char_result;
  int add_overflow_signed_char_max_plus_zero_as_signed_char_overflow;
  add_overflow_signed_char_max_plus_zero_as_signed_char_overflow = __builtin_add_overflow(signed_char_max, 0, &add_overflow_signed_char_max_plus_zero_as_signed_char_result);

  // 127 + 0 = 127, which fits the destination range; stored result = 127 and overflow = 0.
  if (!(add_overflow_signed_char_max_plus_zero_as_signed_char_result == signed_char_max))
    goto ERROR;

  if (!(add_overflow_signed_char_max_plus_zero_as_signed_char_overflow == 0))
    goto ERROR;


  signed char add_overflow_signed_char_max_plus_one_as_signed_char_result;
  int add_overflow_signed_char_max_plus_one_as_signed_char_overflow;
  add_overflow_signed_char_max_plus_one_as_signed_char_overflow = __builtin_add_overflow(signed_char_max, 1, &add_overflow_signed_char_max_plus_one_as_signed_char_result);

  // 127 + 1 = 128, outside the destination range; stored result = -128 and overflow = 1.
  if (!(add_overflow_signed_char_max_plus_one_as_signed_char_result == signed_char_min))
    goto ERROR;

  if (!(add_overflow_signed_char_max_plus_one_as_signed_char_overflow == 1))
    goto ERROR;


  unsigned char add_overflow_unsigned_char_max_plus_one_as_unsigned_char_result;
  int add_overflow_unsigned_char_max_plus_one_as_unsigned_char_overflow;
  add_overflow_unsigned_char_max_plus_one_as_unsigned_char_overflow = __builtin_add_overflow(unsigned_char_max, 1, &add_overflow_unsigned_char_max_plus_one_as_unsigned_char_result);

  // 255U + 1 = 256, outside the destination range; stored result = 0 and overflow = 1.
  if (!(add_overflow_unsigned_char_max_plus_one_as_unsigned_char_result == 0))
    goto ERROR;

  if (!(add_overflow_unsigned_char_max_plus_one_as_unsigned_char_overflow == 1))
    goto ERROR;


  short int add_overflow_short_min_plus_minus_one_as_short_int_result;
  int add_overflow_short_min_plus_minus_one_as_short_int_overflow;
  add_overflow_short_min_plus_minus_one_as_short_int_overflow = __builtin_add_overflow(short_min, -1, &add_overflow_short_min_plus_minus_one_as_short_int_result);

  // -32768 + -1 = -32769, outside the destination range; stored result = 32767 and overflow = 1.
  if (!(add_overflow_short_min_plus_minus_one_as_short_int_result == short_max))
    goto ERROR;

  if (!(add_overflow_short_min_plus_minus_one_as_short_int_overflow == 1))
    goto ERROR;


  unsigned short int add_overflow_minus_one_plus_one_as_unsigned_short_int_result;
  int add_overflow_minus_one_plus_one_as_unsigned_short_int_overflow;
  add_overflow_minus_one_plus_one_as_unsigned_short_int_overflow = __builtin_add_overflow(-1, 1U, &add_overflow_minus_one_plus_one_as_unsigned_short_int_result);

  // -1 + 1U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(add_overflow_minus_one_plus_one_as_unsigned_short_int_result == 0U))
    goto ERROR;

  if (!(add_overflow_minus_one_plus_one_as_unsigned_short_int_overflow == 0))
    goto ERROR;


  int add_overflow_int_max_plus_one_as_int_result;
  int add_overflow_int_max_plus_one_as_int_overflow;
  add_overflow_int_max_plus_one_as_int_overflow = __builtin_add_overflow(int_max, 1, &add_overflow_int_max_plus_one_as_int_result);

  // 2147483647 + 1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(add_overflow_int_max_plus_one_as_int_result == int_min))
    goto ERROR;

  if (!(add_overflow_int_max_plus_one_as_int_overflow == 1))
    goto ERROR;


  unsigned int add_overflow_unsigned_int_max_plus_one_as_unsigned_int_result;
  int add_overflow_unsigned_int_max_plus_one_as_unsigned_int_overflow;
  add_overflow_unsigned_int_max_plus_one_as_unsigned_int_overflow = __builtin_add_overflow(unsigned_int_max, 1U, &add_overflow_unsigned_int_max_plus_one_as_unsigned_int_result);

  // 4294967295U + 1U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  if (!(add_overflow_unsigned_int_max_plus_one_as_unsigned_int_result == 0U))
    goto ERROR;

  if (!(add_overflow_unsigned_int_max_plus_one_as_unsigned_int_overflow == 1))
    goto ERROR;


  long int add_overflow_long_max_plus_one_as_long_int_result;
  int add_overflow_long_max_plus_one_as_long_int_overflow;
  add_overflow_long_max_plus_one_as_long_int_overflow = __builtin_add_overflow(add_overflow_long_max, 1L, &add_overflow_long_max_plus_one_as_long_int_result);

  // 2147483647L + 1L = 2147483648, outside the destination range; stored result = -2147483648L and overflow = 1.
  if (!(add_overflow_long_max_plus_one_as_long_int_result == add_overflow_long_min))
    goto ERROR;

  if (!(add_overflow_long_max_plus_one_as_long_int_overflow == 1))
    goto ERROR;


  unsigned long int add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result;
  int add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_overflow;
  add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_overflow = __builtin_add_overflow(add_overflow_unsigned_long_max, 1UL, &add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result);

  // 4294967295UL + 1UL = 4294967296, outside the destination range; stored result = 0UL and overflow = 1.
  if (!(add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result == 0UL))
    goto ERROR;

  if (!(add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_overflow == 1))
    goto ERROR;


  long long int add_overflow_long_long_min_plus_minus_one_as_long_long_int_result;
  int add_overflow_long_long_min_plus_minus_one_as_long_long_int_overflow;
  add_overflow_long_long_min_plus_minus_one_as_long_long_int_overflow = __builtin_add_overflow(long_long_min, -1LL, &add_overflow_long_long_min_plus_minus_one_as_long_long_int_result);

  // -9223372036854775808LL + -1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  if (!(add_overflow_long_long_min_plus_minus_one_as_long_long_int_result == long_long_max))
    goto ERROR;

  if (!(add_overflow_long_long_min_plus_minus_one_as_long_long_int_overflow == 1))
    goto ERROR;


  unsigned long long int add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_result;
  int add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_overflow;
  add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_overflow = __builtin_add_overflow(unsigned_long_long_max, 1ULL, &add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_result);

  // 18446744073709551615ULL + 1ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  if (!(add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_result == 0ULL))
    goto ERROR;

  if (!(add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_overflow == 1))
    goto ERROR;


  char add_overflow_char_max_plus_one_as_char_result;
  int add_overflow_char_max_plus_one_as_char_overflow;
  add_overflow_char_max_plus_one_as_char_overflow = __builtin_add_overflow(char_max, 1, &add_overflow_char_max_plus_one_as_char_result);

  // (char)127 + 1 = 128, outside the destination range; stored result = (char)-128 and overflow = 1.
  if (!(add_overflow_char_max_plus_one_as_char_result == char_min))
    goto ERROR;

  if (!(add_overflow_char_max_plus_one_as_char_overflow == 1))
    goto ERROR;


  int add_overflow_minus_one_plus_one_as_int_result;
  int add_overflow_minus_one_plus_one_as_int_overflow;
  add_overflow_minus_one_plus_one_as_int_overflow = __builtin_add_overflow(-1LL, 1ULL, &add_overflow_minus_one_plus_one_as_int_result);

  // -1LL + 1ULL = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(add_overflow_minus_one_plus_one_as_int_result == 0))
    goto ERROR;

  if (!(add_overflow_minus_one_plus_one_as_int_overflow == 0))
    goto ERROR;


  int add_overflow_unsigned_long_long_max_plus_zero_as_int_result;
  int add_overflow_unsigned_long_long_max_plus_zero_as_int_overflow;
  add_overflow_unsigned_long_long_max_plus_zero_as_int_overflow = __builtin_add_overflow(unsigned_long_long_max, 0, &add_overflow_unsigned_long_long_max_plus_zero_as_int_result);

  // 18446744073709551615ULL + 0 = 18446744073709551615, outside the destination range; stored result = (int)18446744073709551615ULL and
  // overflow = 1.
  if (!(add_overflow_unsigned_long_long_max_plus_zero_as_int_result == (int)unsigned_long_long_max))
    goto ERROR;

  if (!(add_overflow_unsigned_long_long_max_plus_zero_as_int_overflow == 1))
    goto ERROR;


  // Signed int addition overflow tests.

  int sadd_overflow_zero_plus_zero_result;
  int sadd_overflow_zero_plus_zero_overflow;
  sadd_overflow_zero_plus_zero_overflow = __builtin_sadd_overflow(0, 0, &sadd_overflow_zero_plus_zero_result);

  // 0 + 0 = 0, which fits the destination range; stored result = 0 and overflow = 0.
  if (!(sadd_overflow_zero_plus_zero_result == 0))
    goto ERROR;

  if (!(sadd_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_max_plus_zero_result;
  int sadd_overflow_int_max_plus_zero_overflow;
  sadd_overflow_int_max_plus_zero_overflow = __builtin_sadd_overflow(int_max, 0, &sadd_overflow_int_max_plus_zero_result);

  // 2147483647 + 0 = 2147483647, which fits the destination range; stored result = 2147483647 and overflow = 0.
  if (!(sadd_overflow_int_max_plus_zero_result == int_max))
    goto ERROR;

  if (!(sadd_overflow_int_max_plus_zero_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_min_plus_zero_result;
  int sadd_overflow_int_min_plus_zero_overflow;
  sadd_overflow_int_min_plus_zero_overflow = __builtin_sadd_overflow(int_min, 0, &sadd_overflow_int_min_plus_zero_result);

  // -2147483648 + 0 = -2147483648, which fits the destination range; stored result = -2147483648 and overflow = 0.
  if (!(sadd_overflow_int_min_plus_zero_result == int_min))
    goto ERROR;

  if (!(sadd_overflow_int_min_plus_zero_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_max_plus_minus_one_result;
  int sadd_overflow_int_max_plus_minus_one_overflow;
  sadd_overflow_int_max_plus_minus_one_overflow = __builtin_sadd_overflow(int_max, -1, &sadd_overflow_int_max_plus_minus_one_result);

  // 2147483647 + -1 = 2147483646, which fits the destination range; stored result = 2147483646 and overflow = 0.
  if (!(sadd_overflow_int_max_plus_minus_one_result == 2147483646))
    goto ERROR;

  if (!(sadd_overflow_int_max_plus_minus_one_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_min_plus_one_result;
  int sadd_overflow_int_min_plus_one_overflow;
  sadd_overflow_int_min_plus_one_overflow = __builtin_sadd_overflow(int_min, 1, &sadd_overflow_int_min_plus_one_result);

  // -2147483648 + 1 = -2147483647, which fits the destination range; stored result = -2147483647 and overflow = 0.
  if (!(sadd_overflow_int_min_plus_one_result == -int_max))
    goto ERROR;

  if (!(sadd_overflow_int_min_plus_one_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_max_plus_one_result;
  int sadd_overflow_int_max_plus_one_overflow;
  sadd_overflow_int_max_plus_one_overflow = __builtin_sadd_overflow(int_max, 1, &sadd_overflow_int_max_plus_one_result);

  // 2147483647 + 1 = 2147483648, outside the destination range; stored result = -2147483648 and overflow = 1.
  if (!(sadd_overflow_int_max_plus_one_result == int_min))
    goto ERROR;

  if (!(sadd_overflow_int_max_plus_one_overflow == 1))
    goto ERROR;


  int sadd_overflow_int_min_plus_minus_one_result;
  int sadd_overflow_int_min_plus_minus_one_overflow;
  sadd_overflow_int_min_plus_minus_one_overflow = __builtin_sadd_overflow(int_min, -1, &sadd_overflow_int_min_plus_minus_one_result);

  // -2147483648 + -1 = -2147483649, outside the destination range; stored result = 2147483647 and overflow = 1.
  if (!(sadd_overflow_int_min_plus_minus_one_result == int_max))
    goto ERROR;

  if (!(sadd_overflow_int_min_plus_minus_one_overflow == 1))
    goto ERROR;


  // Signed long int addition overflow tests.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)saddl_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)saddl_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  if (!((sizeof(long int) == sizeof(int) && (int)saddl_overflow_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)saddl_overflow_long_max == (long long int)(long int)long_long_max)))
    goto ERROR;


  // ILP32: The calculated long minimum must equal int minimum -2147483648.
  // LP64: The calculated long minimum must equal long long minimum -9223372036854775808.
  if (!((sizeof(long int) == sizeof(int) && (int)saddl_overflow_long_min == int_min) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)saddl_overflow_long_min ==
            (long long int)(long int)long_long_min)))
    goto ERROR;


  long int saddl_overflow_zero_plus_zero_result;
  int saddl_overflow_zero_plus_zero_overflow;
  saddl_overflow_zero_plus_zero_overflow = __builtin_saddl_overflow(0L, 0L, &saddl_overflow_zero_plus_zero_result);

  // 0L + 0L = 0, which fits the destination range; stored result = 0L and overflow = 0.
  if (!(saddl_overflow_zero_plus_zero_result == 0L))
    goto ERROR;

  if (!(saddl_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_max_plus_zero_result;
  int saddl_overflow_long_max_plus_zero_overflow;
  saddl_overflow_long_max_plus_zero_overflow = __builtin_saddl_overflow(saddl_overflow_long_max, 0L, &saddl_overflow_long_max_plus_zero_result);

  // 2147483647L + 0L = 2147483647, which fits the destination range; stored result = 2147483647L and overflow = 0.
  if (!(saddl_overflow_long_max_plus_zero_result == saddl_overflow_long_max))
    goto ERROR;

  if (!(saddl_overflow_long_max_plus_zero_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_min_plus_zero_result;
  int saddl_overflow_long_min_plus_zero_overflow;
  saddl_overflow_long_min_plus_zero_overflow = __builtin_saddl_overflow(saddl_overflow_long_min, 0L, &saddl_overflow_long_min_plus_zero_result);

  // -2147483648L + 0L = -2147483648, which fits the destination range; stored result = -2147483648L and overflow = 0.
  if (!(saddl_overflow_long_min_plus_zero_result == saddl_overflow_long_min))
    goto ERROR;

  if (!(saddl_overflow_long_min_plus_zero_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_max_plus_minus_one_result;
  int saddl_overflow_long_max_plus_minus_one_overflow;
  saddl_overflow_long_max_plus_minus_one_overflow = __builtin_saddl_overflow(saddl_overflow_long_max, -1L, &saddl_overflow_long_max_plus_minus_one_result);

  // 2147483647L + -1L = 2147483646, which fits the destination range; stored result = 2147483646L and overflow = 0.
  if (!(saddl_overflow_long_max_plus_minus_one_result == ((long int)((~0UL) >> 1)) - 1L))
    goto ERROR;

  if (!(saddl_overflow_long_max_plus_minus_one_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_min_plus_one_result;
  int saddl_overflow_long_min_plus_one_overflow;
  saddl_overflow_long_min_plus_one_overflow = __builtin_saddl_overflow(saddl_overflow_long_min, 1L, &saddl_overflow_long_min_plus_one_result);

  // saddl_overflow_long_min + 1L = saddl_overflow_long_min + 1L, which fits the destination range; stored result = -2147483647L and
  // overflow = 0.
  if (!(saddl_overflow_long_min_plus_one_result == ((-((long int)((~0UL) >> 1)) - 1L)) + 1L))
    goto ERROR;

  if (!(saddl_overflow_long_min_plus_one_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_max_plus_one_result;
  int saddl_overflow_long_max_plus_one_overflow;
  saddl_overflow_long_max_plus_one_overflow = __builtin_saddl_overflow(saddl_overflow_long_max, 1L, &saddl_overflow_long_max_plus_one_result);

  // 2147483647L + 1L = 2147483648, outside the destination range; stored result = -2147483648L and overflow = 1.
  if (!(saddl_overflow_long_max_plus_one_result == saddl_overflow_long_min))
    goto ERROR;

  if (!(saddl_overflow_long_max_plus_one_overflow == 1))
    goto ERROR;


  long int saddl_overflow_long_min_plus_minus_one_result;
  int saddl_overflow_long_min_plus_minus_one_overflow;
  saddl_overflow_long_min_plus_minus_one_overflow = __builtin_saddl_overflow(saddl_overflow_long_min, -1L, &saddl_overflow_long_min_plus_minus_one_result);

  // -2147483648L + -1L = -2147483649, outside the destination range; stored result = 2147483647L and overflow = 1.
  if (!(saddl_overflow_long_min_plus_minus_one_result == saddl_overflow_long_max))
    goto ERROR;

  if (!(saddl_overflow_long_min_plus_minus_one_overflow == 1))
    goto ERROR;


  // Signed long long int addition overflow tests.

  long long int saddll_overflow_zero_plus_zero_result;
  int saddll_overflow_zero_plus_zero_overflow;
  saddll_overflow_zero_plus_zero_overflow = __builtin_saddll_overflow(0LL, 0LL, &saddll_overflow_zero_plus_zero_result);

  // 0LL + 0LL = 0, which fits the destination range; stored result = 0LL and overflow = 0.
  if (!(saddll_overflow_zero_plus_zero_result == 0LL))
    goto ERROR;

  if (!(saddll_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_max_plus_zero_result;
  int saddll_overflow_long_long_max_plus_zero_overflow;
  saddll_overflow_long_long_max_plus_zero_overflow = __builtin_saddll_overflow(long_long_max, 0LL, &saddll_overflow_long_long_max_plus_zero_result);

  // 9223372036854775807LL + 0LL = 9223372036854775807, which fits the destination range; stored result = 9223372036854775807LL and overflow
  // = 0.
  if (!(saddll_overflow_long_long_max_plus_zero_result == long_long_max))
    goto ERROR;

  if (!(saddll_overflow_long_long_max_plus_zero_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_min_plus_zero_result;
  int saddll_overflow_long_long_min_plus_zero_overflow;
  saddll_overflow_long_long_min_plus_zero_overflow = __builtin_saddll_overflow(long_long_min, 0LL, &saddll_overflow_long_long_min_plus_zero_result);

  // -9223372036854775808LL + 0LL = -9223372036854775808, which fits the destination range; stored result = -9223372036854775808LL and
  // overflow = 0.
  if (!(saddll_overflow_long_long_min_plus_zero_result == long_long_min))
    goto ERROR;

  if (!(saddll_overflow_long_long_min_plus_zero_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_max_plus_minus_one_result;
  int saddll_overflow_long_long_max_plus_minus_one_overflow;
  saddll_overflow_long_long_max_plus_minus_one_overflow = __builtin_saddll_overflow(long_long_max, -1LL, &saddll_overflow_long_long_max_plus_minus_one_result);

  // 9223372036854775807LL + -1LL = 9223372036854775806, which fits the destination range; stored result = 9223372036854775806LL and
  // overflow = 0.
  if (!(saddll_overflow_long_long_max_plus_minus_one_result == 9223372036854775806LL))
    goto ERROR;

  if (!(saddll_overflow_long_long_max_plus_minus_one_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_min_plus_one_result;
  int saddll_overflow_long_long_min_plus_one_overflow;
  saddll_overflow_long_long_min_plus_one_overflow = __builtin_saddll_overflow(long_long_min, 1LL, &saddll_overflow_long_long_min_plus_one_result);

  // -9223372036854775808LL + 1LL = -9223372036854775807, which fits the destination range; stored result = -9223372036854775807LL and
  // overflow = 0.
  if (!(saddll_overflow_long_long_min_plus_one_result == -long_long_max))
    goto ERROR;

  if (!(saddll_overflow_long_long_min_plus_one_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_max_plus_one_result;
  int saddll_overflow_long_long_max_plus_one_overflow;
  saddll_overflow_long_long_max_plus_one_overflow = __builtin_saddll_overflow(long_long_max, 1LL, &saddll_overflow_long_long_max_plus_one_result);

  // 9223372036854775807LL + 1LL = 9223372036854775808, outside the destination range; stored result = -9223372036854775808LL and overflow =
  // 1.
  if (!(saddll_overflow_long_long_max_plus_one_result == long_long_min))
    goto ERROR;

  if (!(saddll_overflow_long_long_max_plus_one_overflow == 1))
    goto ERROR;


  long long int saddll_overflow_long_long_min_plus_minus_one_result;
  int saddll_overflow_long_long_min_plus_minus_one_overflow;
  saddll_overflow_long_long_min_plus_minus_one_overflow = __builtin_saddll_overflow(long_long_min, -1LL, &saddll_overflow_long_long_min_plus_minus_one_result);

  // -9223372036854775808LL + -1LL = -9223372036854775809, outside the destination range; stored result = 9223372036854775807LL and overflow
  // = 1.
  if (!(saddll_overflow_long_long_min_plus_minus_one_result == long_long_max))
    goto ERROR;

  if (!(saddll_overflow_long_long_min_plus_minus_one_overflow == 1))
    goto ERROR;


  // Unsigned int addition overflow tests.

  unsigned int uadd_overflow_zero_plus_zero_result;
  int uadd_overflow_zero_plus_zero_overflow;
  uadd_overflow_zero_plus_zero_overflow = __builtin_uadd_overflow(0U, 0U, &uadd_overflow_zero_plus_zero_result);

  // 0U + 0U = 0, which fits the destination range; stored result = 0U and overflow = 0.
  if (!(uadd_overflow_zero_plus_zero_result == 0U))
    goto ERROR;

  if (!(uadd_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_plus_zero_result;
  int uadd_overflow_unsigned_int_max_plus_zero_overflow;
  uadd_overflow_unsigned_int_max_plus_zero_overflow = __builtin_uadd_overflow(unsigned_int_max, 0U, &uadd_overflow_unsigned_int_max_plus_zero_result);

  // 4294967295U + 0U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  if (!(uadd_overflow_unsigned_int_max_plus_zero_result == unsigned_int_max))
    goto ERROR;

  if (!(uadd_overflow_unsigned_int_max_plus_zero_overflow == 0))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_minus_one_plus_one_result;
  int uadd_overflow_unsigned_int_max_minus_one_plus_one_overflow;
  uadd_overflow_unsigned_int_max_minus_one_plus_one_overflow = __builtin_uadd_overflow(4294967294U, 1U, &uadd_overflow_unsigned_int_max_minus_one_plus_one_result);

  // 4294967294U + 1U = 4294967295, which fits the destination range; stored result = 4294967295U and overflow = 0.
  if (!(uadd_overflow_unsigned_int_max_minus_one_plus_one_result == unsigned_int_max))
    goto ERROR;

  if (!(uadd_overflow_unsigned_int_max_minus_one_plus_one_overflow == 0))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_plus_one_result;
  int uadd_overflow_unsigned_int_max_plus_one_overflow;
  uadd_overflow_unsigned_int_max_plus_one_overflow = __builtin_uadd_overflow(unsigned_int_max, 1U, &uadd_overflow_unsigned_int_max_plus_one_result);

  // 4294967295U + 1U = 4294967296, outside the destination range; stored result = 0U and overflow = 1.
  if (!(uadd_overflow_unsigned_int_max_plus_one_result == 0U))
    goto ERROR;

  if (!(uadd_overflow_unsigned_int_max_plus_one_overflow == 1))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_plus_two_result;
  int uadd_overflow_unsigned_int_max_plus_two_overflow;
  uadd_overflow_unsigned_int_max_plus_two_overflow = __builtin_uadd_overflow(unsigned_int_max, 2U, &uadd_overflow_unsigned_int_max_plus_two_result);

  // 4294967295U + 2U = 4294967297, outside the destination range; stored result = 1U and overflow = 1.
  if (!(uadd_overflow_unsigned_int_max_plus_two_result == 1U))
    goto ERROR;

  if (!(uadd_overflow_unsigned_int_max_plus_two_overflow == 1))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_plus_unsigned_int_max_result;
  int uadd_overflow_unsigned_int_max_plus_unsigned_int_max_overflow;
  uadd_overflow_unsigned_int_max_plus_unsigned_int_max_overflow = __builtin_uadd_overflow(unsigned_int_max, unsigned_int_max, &uadd_overflow_unsigned_int_max_plus_unsigned_int_max_result);

  // 4294967295U + 4294967295U = 8589934590, outside the destination range; stored result = 4294967294U and overflow = 1.
  if (!(uadd_overflow_unsigned_int_max_plus_unsigned_int_max_result == 4294967294U))
    goto ERROR;

  if (!(uadd_overflow_unsigned_int_max_plus_unsigned_int_max_overflow == 1))
    goto ERROR;


  // Unsigned long int addition overflow tests.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)uaddl_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)uaddl_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  unsigned long int uaddl_overflow_zero_plus_zero_result;
  int uaddl_overflow_zero_plus_zero_overflow;
  uaddl_overflow_zero_plus_zero_overflow = __builtin_uaddl_overflow(0UL, 0UL, &uaddl_overflow_zero_plus_zero_result);

  // 0UL + 0UL = 0, which fits the destination range; stored result = 0UL and overflow = 0.
  if (!(uaddl_overflow_zero_plus_zero_result == 0UL))
    goto ERROR;

  if (!(uaddl_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_plus_zero_result;
  int uaddl_overflow_unsigned_long_max_plus_zero_overflow;
  uaddl_overflow_unsigned_long_max_plus_zero_overflow = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 0UL, &uaddl_overflow_unsigned_long_max_plus_zero_result);

  // 4294967295UL + 0UL = 4294967295, which fits the destination range; stored result = 4294967295UL and overflow = 0.
  if (!(uaddl_overflow_unsigned_long_max_plus_zero_result == uaddl_overflow_unsigned_long_max))
    goto ERROR;

  if (!(uaddl_overflow_unsigned_long_max_plus_zero_overflow == 0))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_minus_one_plus_one_result;
  int uaddl_overflow_unsigned_long_max_minus_one_plus_one_overflow;
  uaddl_overflow_unsigned_long_max_minus_one_plus_one_overflow = __builtin_uaddl_overflow((~0UL) - 1UL, 1UL, &uaddl_overflow_unsigned_long_max_minus_one_plus_one_result);

  // 4294967294UL + 1UL = 4294967295, which fits the destination range; stored result = 4294967295UL and overflow = 0.
  if (!(uaddl_overflow_unsigned_long_max_minus_one_plus_one_result == uaddl_overflow_unsigned_long_max))
    goto ERROR;

  if (!(uaddl_overflow_unsigned_long_max_minus_one_plus_one_overflow == 0))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_plus_one_result;
  int uaddl_overflow_unsigned_long_max_plus_one_overflow;
  uaddl_overflow_unsigned_long_max_plus_one_overflow = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 1UL, &uaddl_overflow_unsigned_long_max_plus_one_result);

  // 4294967295UL + 1UL = 4294967296, outside the destination range; stored result = 0UL and overflow = 1.
  if (!(uaddl_overflow_unsigned_long_max_plus_one_result == 0UL))
    goto ERROR;

  if (!(uaddl_overflow_unsigned_long_max_plus_one_overflow == 1))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_plus_two_result;
  int uaddl_overflow_unsigned_long_max_plus_two_overflow;
  uaddl_overflow_unsigned_long_max_plus_two_overflow = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 2UL, &uaddl_overflow_unsigned_long_max_plus_two_result);

  // 4294967295UL + 2UL = 4294967297, outside the destination range; stored result = 1UL and overflow = 1.
  if (!(uaddl_overflow_unsigned_long_max_plus_two_result == 1UL))
    goto ERROR;

  if (!(uaddl_overflow_unsigned_long_max_plus_two_overflow == 1))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result;
  int uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_overflow;
  uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_overflow = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, uaddl_overflow_unsigned_long_max, &uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result);

  // 4294967295UL + 4294967295UL = 8589934590, outside the destination range; stored result = 4294967294UL and overflow = 1.
  if (!(uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result == (~0UL) - 1UL))
    goto ERROR;

  if (!(uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_overflow == 1))
    goto ERROR;


  // Unsigned long long int addition overflow tests.

  unsigned long long int uaddll_overflow_zero_plus_zero_result;
  int uaddll_overflow_zero_plus_zero_overflow;
  uaddll_overflow_zero_plus_zero_overflow = __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_overflow_zero_plus_zero_result);

  // 0ULL + 0ULL = 0, which fits the destination range; stored result = 0ULL and overflow = 0.
  if (!(uaddll_overflow_zero_plus_zero_result == 0ULL))
    goto ERROR;

  if (!(uaddll_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_plus_zero_result;
  int uaddll_overflow_unsigned_long_long_max_plus_zero_overflow;
  uaddll_overflow_unsigned_long_long_max_plus_zero_overflow = __builtin_uaddll_overflow(unsigned_long_long_max, 0ULL, &uaddll_overflow_unsigned_long_long_max_plus_zero_result);

  // 18446744073709551615ULL + 0ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_zero_result == unsigned_long_long_max))
    goto ERROR;

  if (!(uaddll_overflow_unsigned_long_long_max_plus_zero_overflow == 0))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_result;
  int uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_overflow;
  uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_overflow = __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_result);

  // 18446744073709551614ULL + 1ULL = 18446744073709551615, which fits the destination range; stored result = 18446744073709551615ULL and
  // overflow = 0.
  if (!(uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_result == unsigned_long_long_max))
    goto ERROR;

  if (!(uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_overflow == 0))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_plus_one_result;
  int uaddll_overflow_unsigned_long_long_max_plus_one_overflow;
  uaddll_overflow_unsigned_long_long_max_plus_one_overflow = __builtin_uaddll_overflow(unsigned_long_long_max, 1ULL, &uaddll_overflow_unsigned_long_long_max_plus_one_result);

  // 18446744073709551615ULL + 1ULL = 18446744073709551616, outside the destination range; stored result = 0ULL and overflow = 1.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_one_result == 0ULL))
    goto ERROR;

  if (!(uaddll_overflow_unsigned_long_long_max_plus_one_overflow == 1))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_plus_two_result;
  int uaddll_overflow_unsigned_long_long_max_plus_two_overflow;
  uaddll_overflow_unsigned_long_long_max_plus_two_overflow = __builtin_uaddll_overflow(unsigned_long_long_max, 2ULL, &uaddll_overflow_unsigned_long_long_max_plus_two_result);

  // 18446744073709551615ULL + 2ULL = 18446744073709551617, outside the destination range; stored result = 1ULL and overflow = 1.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_two_result == 1ULL))
    goto ERROR;

  if (!(uaddll_overflow_unsigned_long_long_max_plus_two_overflow == 1))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_result;
  int uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_overflow;
  uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_overflow = __builtin_uaddll_overflow(unsigned_long_long_max, unsigned_long_long_max, &uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_result);

  // 18446744073709551615ULL + 18446744073709551615ULL = 36893488147419103230, outside the destination range; stored result =
  // 18446744073709551614ULL and overflow = 1.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_result == 18446744073709551614ULL))
    goto ERROR;

  if (!(uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_overflow == 1))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
