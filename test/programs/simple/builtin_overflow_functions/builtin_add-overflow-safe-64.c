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


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: 2147483647L + 1L fits in long, stores 2147483648L, and returns 0.
  long int model_saddl_result;
  int model_saddl_overflow;
  model_saddl_overflow = __builtin_saddl_overflow(2147483647L, 1L, &model_saddl_result);

  if (!(model_saddl_result != 0L && model_saddl_overflow == 0))
    goto ERROR;

  // Tests for __builtin_add_overflow.


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

  // __builtin_add_overflow(127, 0, &add_overflow_signed_char_max_plus_zero_as_signed_char_result) stores 127.
  if (!(add_overflow_signed_char_max_plus_zero_as_signed_char_result == signed_char_max))
    goto ERROR;

  // __builtin_add_overflow(127, 0, &add_overflow_signed_char_max_plus_zero_as_signed_char_result) returns 0.
  if (!(add_overflow_signed_char_max_plus_zero_as_signed_char_overflow == 0))
    goto ERROR;


  signed char add_overflow_signed_char_max_plus_one_as_signed_char_result;
  int add_overflow_signed_char_max_plus_one_as_signed_char_overflow;
  add_overflow_signed_char_max_plus_one_as_signed_char_overflow = __builtin_add_overflow(signed_char_max, 1, &add_overflow_signed_char_max_plus_one_as_signed_char_result);

  // __builtin_add_overflow(127, 1, &add_overflow_signed_char_max_plus_one_as_signed_char_result) stores -128.
  if (!(add_overflow_signed_char_max_plus_one_as_signed_char_result == signed_char_min))
    goto ERROR;

  // __builtin_add_overflow(127, 1, &add_overflow_signed_char_max_plus_one_as_signed_char_result) returns 1.
  if (!(add_overflow_signed_char_max_plus_one_as_signed_char_overflow == 1))
    goto ERROR;


  unsigned char add_overflow_unsigned_char_max_plus_one_as_unsigned_char_result;
  int add_overflow_unsigned_char_max_plus_one_as_unsigned_char_overflow;
  add_overflow_unsigned_char_max_plus_one_as_unsigned_char_overflow = __builtin_add_overflow(unsigned_char_max, 1, &add_overflow_unsigned_char_max_plus_one_as_unsigned_char_result);

  // __builtin_add_overflow(255U, 1, &add_overflow_unsigned_char_max_plus_one_as_unsigned_char_result) stores 0.
  if (!(add_overflow_unsigned_char_max_plus_one_as_unsigned_char_result == 0))
    goto ERROR;

  // __builtin_add_overflow(255U, 1, &add_overflow_unsigned_char_max_plus_one_as_unsigned_char_result) returns 1.
  if (!(add_overflow_unsigned_char_max_plus_one_as_unsigned_char_overflow == 1))
    goto ERROR;


  short int add_overflow_short_min_plus_minus_one_as_short_int_result;
  int add_overflow_short_min_plus_minus_one_as_short_int_overflow;
  add_overflow_short_min_plus_minus_one_as_short_int_overflow = __builtin_add_overflow(short_min, -1, &add_overflow_short_min_plus_minus_one_as_short_int_result);

  // __builtin_add_overflow(-32768, -1, &add_overflow_short_min_plus_minus_one_as_short_int_result) stores 32767.
  if (!(add_overflow_short_min_plus_minus_one_as_short_int_result == short_max))
    goto ERROR;

  // __builtin_add_overflow(-32768, -1, &add_overflow_short_min_plus_minus_one_as_short_int_result) returns 1.
  if (!(add_overflow_short_min_plus_minus_one_as_short_int_overflow == 1))
    goto ERROR;


  unsigned short int add_overflow_minus_one_plus_one_as_unsigned_short_int_result;
  int add_overflow_minus_one_plus_one_as_unsigned_short_int_overflow;
  add_overflow_minus_one_plus_one_as_unsigned_short_int_overflow = __builtin_add_overflow(-1, 1U, &add_overflow_minus_one_plus_one_as_unsigned_short_int_result);

  // __builtin_add_overflow(-1, 1U, &add_overflow_minus_one_plus_one_as_unsigned_short_int_result) stores 0U.
  if (!(add_overflow_minus_one_plus_one_as_unsigned_short_int_result == 0U))
    goto ERROR;

  // __builtin_add_overflow(-1, 1U, &add_overflow_minus_one_plus_one_as_unsigned_short_int_result) returns 0.
  if (!(add_overflow_minus_one_plus_one_as_unsigned_short_int_overflow == 0))
    goto ERROR;


  int add_overflow_int_max_plus_one_as_int_result;
  int add_overflow_int_max_plus_one_as_int_overflow;
  add_overflow_int_max_plus_one_as_int_overflow = __builtin_add_overflow(int_max, 1, &add_overflow_int_max_plus_one_as_int_result);

  // __builtin_add_overflow(2147483647, 1, &add_overflow_int_max_plus_one_as_int_result) stores (-2147483647 - 1).
  if (!(add_overflow_int_max_plus_one_as_int_result == int_min))
    goto ERROR;

  // __builtin_add_overflow(2147483647, 1, &add_overflow_int_max_plus_one_as_int_result) returns 1.
  if (!(add_overflow_int_max_plus_one_as_int_overflow == 1))
    goto ERROR;


  unsigned int add_overflow_unsigned_int_max_plus_one_as_unsigned_int_result;
  int add_overflow_unsigned_int_max_plus_one_as_unsigned_int_overflow;
  add_overflow_unsigned_int_max_plus_one_as_unsigned_int_overflow = __builtin_add_overflow(unsigned_int_max, 1U, &add_overflow_unsigned_int_max_plus_one_as_unsigned_int_result);

  // __builtin_add_overflow(4294967295U, 1U, &add_overflow_unsigned_int_max_plus_one_as_unsigned_int_result) stores 0U.
  if (!(add_overflow_unsigned_int_max_plus_one_as_unsigned_int_result == 0U))
    goto ERROR;

  // __builtin_add_overflow(4294967295U, 1U, &add_overflow_unsigned_int_max_plus_one_as_unsigned_int_result) returns 1.
  if (!(add_overflow_unsigned_int_max_plus_one_as_unsigned_int_overflow == 1))
    goto ERROR;


  long int add_overflow_long_max_plus_one_as_long_int_result;
  int add_overflow_long_max_plus_one_as_long_int_overflow;
  add_overflow_long_max_plus_one_as_long_int_overflow = __builtin_add_overflow(add_overflow_long_max, 1L, &add_overflow_long_max_plus_one_as_long_int_result);

  // ILP32: __builtin_add_overflow(2147483647L, 1L, &add_overflow_long_max_plus_one_as_long_int_result) stores -2147483648L.
  // LP64: __builtin_add_overflow(9223372036854775807L, 1L, &add_overflow_long_max_plus_one_as_long_int_result) stores
  // -9223372036854775808L.
  if (!(add_overflow_long_max_plus_one_as_long_int_result == add_overflow_long_min))
    goto ERROR;

  // ILP32: __builtin_add_overflow(2147483647L, 1L, &add_overflow_long_max_plus_one_as_long_int_result) returns 1.
  // LP64: __builtin_add_overflow(9223372036854775807L, 1L, &add_overflow_long_max_plus_one_as_long_int_result) returns 1.
  if (!(add_overflow_long_max_plus_one_as_long_int_overflow == 1))
    goto ERROR;


  unsigned long int add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result;
  int add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_overflow;
  add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_overflow = __builtin_add_overflow(add_overflow_unsigned_long_max, 1UL, &add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result);

  // ILP32: __builtin_add_overflow(4294967295UL, 1UL, &add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result) stores 0UL.
  // LP64: __builtin_add_overflow(18446744073709551615UL, 1UL, &add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result) stores
  // 0UL.
  if (!(add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result == 0UL))
    goto ERROR;

  // ILP32: __builtin_add_overflow(4294967295UL, 1UL, &add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result) returns 1.
  // LP64: __builtin_add_overflow(18446744073709551615UL, 1UL, &add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_result) returns
  // 1.
  if (!(add_overflow_unsigned_long_max_plus_one_as_unsigned_long_int_overflow == 1))
    goto ERROR;


  long long int add_overflow_long_long_min_plus_minus_one_as_long_long_int_result;
  int add_overflow_long_long_min_plus_minus_one_as_long_long_int_overflow;
  add_overflow_long_long_min_plus_minus_one_as_long_long_int_overflow = __builtin_add_overflow(long_long_min, -1LL, &add_overflow_long_long_min_plus_minus_one_as_long_long_int_result);

  // __builtin_add_overflow(-9223372036854775808LL, -1LL, &add_overflow_long_long_min_plus_minus_one_as_long_long_int_result) stores
  // 9223372036854775807LL.
  if (!(add_overflow_long_long_min_plus_minus_one_as_long_long_int_result == long_long_max))
    goto ERROR;

  // __builtin_add_overflow(-9223372036854775808LL, -1LL, &add_overflow_long_long_min_plus_minus_one_as_long_long_int_result) returns 1.
  if (!(add_overflow_long_long_min_plus_minus_one_as_long_long_int_overflow == 1))
    goto ERROR;


  unsigned long long int add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_result;
  int add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_overflow;
  add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_overflow = __builtin_add_overflow(unsigned_long_long_max, 1ULL, &add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_result);

  // __builtin_add_overflow(18446744073709551615ULL, 1ULL, &add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_result)
  // stores 0ULL.
  if (!(add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_result == 0ULL))
    goto ERROR;

  // __builtin_add_overflow(18446744073709551615ULL, 1ULL, &add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_result)
  // returns 1.
  if (!(add_overflow_unsigned_long_long_max_plus_one_as_unsigned_long_long_int_overflow == 1))
    goto ERROR;


  char add_overflow_char_max_plus_one_as_char_result;
  int add_overflow_char_max_plus_one_as_char_overflow;
  add_overflow_char_max_plus_one_as_char_overflow = __builtin_add_overflow(char_max, 1, &add_overflow_char_max_plus_one_as_char_result);

  // __builtin_add_overflow((char)127, 1, &add_overflow_char_max_plus_one_as_char_result) stores (char)-128.
  if (!(add_overflow_char_max_plus_one_as_char_result == char_min))
    goto ERROR;

  // __builtin_add_overflow((char)127, 1, &add_overflow_char_max_plus_one_as_char_result) returns 1.
  if (!(add_overflow_char_max_plus_one_as_char_overflow == 1))
    goto ERROR;


  int add_overflow_minus_one_plus_one_as_int_result;
  int add_overflow_minus_one_plus_one_as_int_overflow;
  add_overflow_minus_one_plus_one_as_int_overflow = __builtin_add_overflow(-1LL, 1ULL, &add_overflow_minus_one_plus_one_as_int_result);

  // __builtin_add_overflow(-1LL, 1ULL, &add_overflow_minus_one_plus_one_as_int_result) stores 0.
  if (!(add_overflow_minus_one_plus_one_as_int_result == 0))
    goto ERROR;

  // __builtin_add_overflow(-1LL, 1ULL, &add_overflow_minus_one_plus_one_as_int_result) returns 0.
  if (!(add_overflow_minus_one_plus_one_as_int_overflow == 0))
    goto ERROR;


  int add_overflow_unsigned_long_long_max_plus_zero_as_int_result;
  int add_overflow_unsigned_long_long_max_plus_zero_as_int_overflow;
  add_overflow_unsigned_long_long_max_plus_zero_as_int_overflow = __builtin_add_overflow(unsigned_long_long_max, 0, &add_overflow_unsigned_long_long_max_plus_zero_as_int_result);

  // __builtin_add_overflow(18446744073709551615ULL, 0, &add_overflow_unsigned_long_long_max_plus_zero_as_int_result) stores
  // (int)18446744073709551615ULL.
  if (!(add_overflow_unsigned_long_long_max_plus_zero_as_int_result == (int)unsigned_long_long_max))
    goto ERROR;

  // __builtin_add_overflow(18446744073709551615ULL, 0, &add_overflow_unsigned_long_long_max_plus_zero_as_int_result) returns 1.
  if (!(add_overflow_unsigned_long_long_max_plus_zero_as_int_overflow == 1))
    goto ERROR;


  // Tests for __builtin_sadd_overflow.

  int sadd_overflow_zero_plus_zero_result;
  int sadd_overflow_zero_plus_zero_overflow;
  sadd_overflow_zero_plus_zero_overflow = __builtin_sadd_overflow(0, 0, &sadd_overflow_zero_plus_zero_result);

  // __builtin_sadd_overflow(0, 0, &sadd_overflow_zero_plus_zero_result) stores 0.
  if (!(sadd_overflow_zero_plus_zero_result == 0))
    goto ERROR;

  // __builtin_sadd_overflow(0, 0, &sadd_overflow_zero_plus_zero_result) returns 0.
  if (!(sadd_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_max_plus_zero_result;
  int sadd_overflow_int_max_plus_zero_overflow;
  sadd_overflow_int_max_plus_zero_overflow = __builtin_sadd_overflow(int_max, 0, &sadd_overflow_int_max_plus_zero_result);

  // __builtin_sadd_overflow(2147483647, 0, &sadd_overflow_int_max_plus_zero_result) stores 2147483647.
  if (!(sadd_overflow_int_max_plus_zero_result == int_max))
    goto ERROR;

  // __builtin_sadd_overflow(2147483647, 0, &sadd_overflow_int_max_plus_zero_result) returns 0.
  if (!(sadd_overflow_int_max_plus_zero_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_min_plus_zero_result;
  int sadd_overflow_int_min_plus_zero_overflow;
  sadd_overflow_int_min_plus_zero_overflow = __builtin_sadd_overflow(int_min, 0, &sadd_overflow_int_min_plus_zero_result);

  // __builtin_sadd_overflow((-2147483647 - 1), 0, &sadd_overflow_int_min_plus_zero_result) stores (-2147483647 - 1).
  if (!(sadd_overflow_int_min_plus_zero_result == int_min))
    goto ERROR;

  // __builtin_sadd_overflow((-2147483647 - 1), 0, &sadd_overflow_int_min_plus_zero_result) returns 0.
  if (!(sadd_overflow_int_min_plus_zero_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_max_plus_minus_one_result;
  int sadd_overflow_int_max_plus_minus_one_overflow;
  sadd_overflow_int_max_plus_minus_one_overflow = __builtin_sadd_overflow(int_max, -1, &sadd_overflow_int_max_plus_minus_one_result);

  // __builtin_sadd_overflow(2147483647, -1, &sadd_overflow_int_max_plus_minus_one_result) stores 2147483647 - 1.
  if (!(sadd_overflow_int_max_plus_minus_one_result == 2147483646))
    goto ERROR;

  // __builtin_sadd_overflow(2147483647, -1, &sadd_overflow_int_max_plus_minus_one_result) returns 0.
  if (!(sadd_overflow_int_max_plus_minus_one_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_min_plus_one_result;
  int sadd_overflow_int_min_plus_one_overflow;
  sadd_overflow_int_min_plus_one_overflow = __builtin_sadd_overflow(int_min, 1, &sadd_overflow_int_min_plus_one_result);

  // __builtin_sadd_overflow((-2147483647 - 1), 1, &sadd_overflow_int_min_plus_one_result) stores (-2147483647 - 1) + 1.
  if (!(sadd_overflow_int_min_plus_one_result == -int_max))
    goto ERROR;

  // __builtin_sadd_overflow((-2147483647 - 1), 1, &sadd_overflow_int_min_plus_one_result) returns 0.
  if (!(sadd_overflow_int_min_plus_one_overflow == 0))
    goto ERROR;


  int sadd_overflow_int_max_plus_one_result;
  int sadd_overflow_int_max_plus_one_overflow;
  sadd_overflow_int_max_plus_one_overflow = __builtin_sadd_overflow(int_max, 1, &sadd_overflow_int_max_plus_one_result);

  // __builtin_sadd_overflow(2147483647, 1, &sadd_overflow_int_max_plus_one_result) stores (-2147483647 - 1).
  if (!(sadd_overflow_int_max_plus_one_result == int_min))
    goto ERROR;

  // __builtin_sadd_overflow(2147483647, 1, &sadd_overflow_int_max_plus_one_result) returns 1.
  if (!(sadd_overflow_int_max_plus_one_overflow == 1))
    goto ERROR;


  int sadd_overflow_int_min_plus_minus_one_result;
  int sadd_overflow_int_min_plus_minus_one_overflow;
  sadd_overflow_int_min_plus_minus_one_overflow = __builtin_sadd_overflow(int_min, -1, &sadd_overflow_int_min_plus_minus_one_result);

  // __builtin_sadd_overflow((-2147483647 - 1), -1, &sadd_overflow_int_min_plus_minus_one_result) stores 2147483647.
  if (!(sadd_overflow_int_min_plus_minus_one_result == int_max))
    goto ERROR;

  // __builtin_sadd_overflow((-2147483647 - 1), -1, &sadd_overflow_int_min_plus_minus_one_result) returns 1.
  if (!(sadd_overflow_int_min_plus_minus_one_overflow == 1))
    goto ERROR;


  // Tests for __builtin_saddl_overflow.


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

  // __builtin_saddl_overflow(0L, 0L, &saddl_overflow_zero_plus_zero_result) stores 0L.
  if (!(saddl_overflow_zero_plus_zero_result == 0L))
    goto ERROR;

  // __builtin_saddl_overflow(0L, 0L, &saddl_overflow_zero_plus_zero_result) returns 0.
  if (!(saddl_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_max_plus_zero_result;
  int saddl_overflow_long_max_plus_zero_overflow;
  saddl_overflow_long_max_plus_zero_overflow = __builtin_saddl_overflow(saddl_overflow_long_max, 0L, &saddl_overflow_long_max_plus_zero_result);

  // ILP32: __builtin_saddl_overflow(2147483647L, 0L, &saddl_overflow_long_max_plus_zero_result) stores 2147483647L.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, 0L, &saddl_overflow_long_max_plus_zero_result) stores 9223372036854775807L.
  if (!(saddl_overflow_long_max_plus_zero_result == saddl_overflow_long_max))
    goto ERROR;

  // ILP32: __builtin_saddl_overflow(2147483647L, 0L, &saddl_overflow_long_max_plus_zero_result) returns 0.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, 0L, &saddl_overflow_long_max_plus_zero_result) returns 0.
  if (!(saddl_overflow_long_max_plus_zero_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_min_plus_zero_result;
  int saddl_overflow_long_min_plus_zero_overflow;
  saddl_overflow_long_min_plus_zero_overflow = __builtin_saddl_overflow(saddl_overflow_long_min, 0L, &saddl_overflow_long_min_plus_zero_result);

  // ILP32: __builtin_saddl_overflow(-2147483648L, 0L, &saddl_overflow_long_min_plus_zero_result) stores -2147483648L.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, 0L, &saddl_overflow_long_min_plus_zero_result) stores -9223372036854775808L.
  if (!(saddl_overflow_long_min_plus_zero_result == saddl_overflow_long_min))
    goto ERROR;

  // ILP32: __builtin_saddl_overflow(-2147483648L, 0L, &saddl_overflow_long_min_plus_zero_result) returns 0.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, 0L, &saddl_overflow_long_min_plus_zero_result) returns 0.
  if (!(saddl_overflow_long_min_plus_zero_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_max_plus_minus_one_result;
  int saddl_overflow_long_max_plus_minus_one_overflow;
  saddl_overflow_long_max_plus_minus_one_overflow = __builtin_saddl_overflow(saddl_overflow_long_max, -1L, &saddl_overflow_long_max_plus_minus_one_result);

  // ILP32: __builtin_saddl_overflow(2147483647L, -1L, &saddl_overflow_long_max_plus_minus_one_result) stores 2147483647L - 1L.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, -1L, &saddl_overflow_long_max_plus_minus_one_result) stores 9223372036854775807L -
  // 1L.
  if (!(saddl_overflow_long_max_plus_minus_one_result == ((long int)((~0UL) >> 1)) - 1L))
    goto ERROR;

  // ILP32: __builtin_saddl_overflow(2147483647L, -1L, &saddl_overflow_long_max_plus_minus_one_result) returns 0.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, -1L, &saddl_overflow_long_max_plus_minus_one_result) returns 0.
  if (!(saddl_overflow_long_max_plus_minus_one_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_min_plus_one_result;
  int saddl_overflow_long_min_plus_one_overflow;
  saddl_overflow_long_min_plus_one_overflow = __builtin_saddl_overflow(saddl_overflow_long_min, 1L, &saddl_overflow_long_min_plus_one_result);

  // ILP32: __builtin_saddl_overflow(-2147483648L, 1L, &saddl_overflow_long_min_plus_one_result) stores -2147483648L + 1L.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, 1L, &saddl_overflow_long_min_plus_one_result) stores -9223372036854775808L + 1L.
  if (!(saddl_overflow_long_min_plus_one_result == ((-((long int)((~0UL) >> 1)) - 1L)) + 1L))
    goto ERROR;

  // ILP32: __builtin_saddl_overflow(-2147483648L, 1L, &saddl_overflow_long_min_plus_one_result) returns 0.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, 1L, &saddl_overflow_long_min_plus_one_result) returns 0.
  if (!(saddl_overflow_long_min_plus_one_overflow == 0))
    goto ERROR;


  long int saddl_overflow_long_max_plus_one_result;
  int saddl_overflow_long_max_plus_one_overflow;
  saddl_overflow_long_max_plus_one_overflow = __builtin_saddl_overflow(saddl_overflow_long_max, 1L, &saddl_overflow_long_max_plus_one_result);

  // ILP32: __builtin_saddl_overflow(2147483647L, 1L, &saddl_overflow_long_max_plus_one_result) stores -2147483648L.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, 1L, &saddl_overflow_long_max_plus_one_result) stores -9223372036854775808L.
  if (!(saddl_overflow_long_max_plus_one_result == saddl_overflow_long_min))
    goto ERROR;

  // ILP32: __builtin_saddl_overflow(2147483647L, 1L, &saddl_overflow_long_max_plus_one_result) returns 1.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, 1L, &saddl_overflow_long_max_plus_one_result) returns 1.
  if (!(saddl_overflow_long_max_plus_one_overflow == 1))
    goto ERROR;


  long int saddl_overflow_long_min_plus_minus_one_result;
  int saddl_overflow_long_min_plus_minus_one_overflow;
  saddl_overflow_long_min_plus_minus_one_overflow = __builtin_saddl_overflow(saddl_overflow_long_min, -1L, &saddl_overflow_long_min_plus_minus_one_result);

  // ILP32: __builtin_saddl_overflow(-2147483648L, -1L, &saddl_overflow_long_min_plus_minus_one_result) stores 2147483647L.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, -1L, &saddl_overflow_long_min_plus_minus_one_result) stores 9223372036854775807L.
  if (!(saddl_overflow_long_min_plus_minus_one_result == saddl_overflow_long_max))
    goto ERROR;

  // ILP32: __builtin_saddl_overflow(-2147483648L, -1L, &saddl_overflow_long_min_plus_minus_one_result) returns 1.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, -1L, &saddl_overflow_long_min_plus_minus_one_result) returns 1.
  if (!(saddl_overflow_long_min_plus_minus_one_overflow == 1))
    goto ERROR;


  // Tests for __builtin_saddll_overflow.

  long long int saddll_overflow_zero_plus_zero_result;
  int saddll_overflow_zero_plus_zero_overflow;
  saddll_overflow_zero_plus_zero_overflow = __builtin_saddll_overflow(0LL, 0LL, &saddll_overflow_zero_plus_zero_result);

  // __builtin_saddll_overflow(0LL, 0LL, &saddll_overflow_zero_plus_zero_result) stores 0LL.
  if (!(saddll_overflow_zero_plus_zero_result == 0LL))
    goto ERROR;

  // __builtin_saddll_overflow(0LL, 0LL, &saddll_overflow_zero_plus_zero_result) returns 0.
  if (!(saddll_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_max_plus_zero_result;
  int saddll_overflow_long_long_max_plus_zero_overflow;
  saddll_overflow_long_long_max_plus_zero_overflow = __builtin_saddll_overflow(long_long_max, 0LL, &saddll_overflow_long_long_max_plus_zero_result);

  // __builtin_saddll_overflow(9223372036854775807LL, 0LL, &saddll_overflow_long_long_max_plus_zero_result) stores 9223372036854775807LL.
  if (!(saddll_overflow_long_long_max_plus_zero_result == long_long_max))
    goto ERROR;

  // __builtin_saddll_overflow(9223372036854775807LL, 0LL, &saddll_overflow_long_long_max_plus_zero_result) returns 0.
  if (!(saddll_overflow_long_long_max_plus_zero_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_min_plus_zero_result;
  int saddll_overflow_long_long_min_plus_zero_overflow;
  saddll_overflow_long_long_min_plus_zero_overflow = __builtin_saddll_overflow(long_long_min, 0LL, &saddll_overflow_long_long_min_plus_zero_result);

  // __builtin_saddll_overflow(-9223372036854775808LL, 0LL, &saddll_overflow_long_long_min_plus_zero_result) stores -9223372036854775808LL.
  if (!(saddll_overflow_long_long_min_plus_zero_result == long_long_min))
    goto ERROR;

  // __builtin_saddll_overflow(-9223372036854775808LL, 0LL, &saddll_overflow_long_long_min_plus_zero_result) returns 0.
  if (!(saddll_overflow_long_long_min_plus_zero_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_max_plus_minus_one_result;
  int saddll_overflow_long_long_max_plus_minus_one_overflow;
  saddll_overflow_long_long_max_plus_minus_one_overflow = __builtin_saddll_overflow(long_long_max, -1LL, &saddll_overflow_long_long_max_plus_minus_one_result);

  // __builtin_saddll_overflow(9223372036854775807LL, -1LL, &saddll_overflow_long_long_max_plus_minus_one_result) stores
  // 9223372036854775807LL - 1LL.
  if (!(saddll_overflow_long_long_max_plus_minus_one_result == 9223372036854775806LL))
    goto ERROR;

  // __builtin_saddll_overflow(9223372036854775807LL, -1LL, &saddll_overflow_long_long_max_plus_minus_one_result) returns 0.
  if (!(saddll_overflow_long_long_max_plus_minus_one_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_min_plus_one_result;
  int saddll_overflow_long_long_min_plus_one_overflow;
  saddll_overflow_long_long_min_plus_one_overflow = __builtin_saddll_overflow(long_long_min, 1LL, &saddll_overflow_long_long_min_plus_one_result);

  // __builtin_saddll_overflow(-9223372036854775808LL, 1LL, &saddll_overflow_long_long_min_plus_one_result) stores -9223372036854775808LL +
  // 1LL.
  if (!(saddll_overflow_long_long_min_plus_one_result == -long_long_max))
    goto ERROR;

  // __builtin_saddll_overflow(-9223372036854775808LL, 1LL, &saddll_overflow_long_long_min_plus_one_result) returns 0.
  if (!(saddll_overflow_long_long_min_plus_one_overflow == 0))
    goto ERROR;


  long long int saddll_overflow_long_long_max_plus_one_result;
  int saddll_overflow_long_long_max_plus_one_overflow;
  saddll_overflow_long_long_max_plus_one_overflow = __builtin_saddll_overflow(long_long_max, 1LL, &saddll_overflow_long_long_max_plus_one_result);

  // __builtin_saddll_overflow(9223372036854775807LL, 1LL, &saddll_overflow_long_long_max_plus_one_result) stores -9223372036854775808LL.
  if (!(saddll_overflow_long_long_max_plus_one_result == long_long_min))
    goto ERROR;

  // __builtin_saddll_overflow(9223372036854775807LL, 1LL, &saddll_overflow_long_long_max_plus_one_result) returns 1.
  if (!(saddll_overflow_long_long_max_plus_one_overflow == 1))
    goto ERROR;


  long long int saddll_overflow_long_long_min_plus_minus_one_result;
  int saddll_overflow_long_long_min_plus_minus_one_overflow;
  saddll_overflow_long_long_min_plus_minus_one_overflow = __builtin_saddll_overflow(long_long_min, -1LL, &saddll_overflow_long_long_min_plus_minus_one_result);

  // __builtin_saddll_overflow(-9223372036854775808LL, -1LL, &saddll_overflow_long_long_min_plus_minus_one_result) stores
  // 9223372036854775807LL.
  if (!(saddll_overflow_long_long_min_plus_minus_one_result == long_long_max))
    goto ERROR;

  // __builtin_saddll_overflow(-9223372036854775808LL, -1LL, &saddll_overflow_long_long_min_plus_minus_one_result) returns 1.
  if (!(saddll_overflow_long_long_min_plus_minus_one_overflow == 1))
    goto ERROR;


  // Tests for __builtin_uadd_overflow.

  unsigned int uadd_overflow_zero_plus_zero_result;
  int uadd_overflow_zero_plus_zero_overflow;
  uadd_overflow_zero_plus_zero_overflow = __builtin_uadd_overflow(0U, 0U, &uadd_overflow_zero_plus_zero_result);

  // __builtin_uadd_overflow(0U, 0U, &uadd_overflow_zero_plus_zero_result) stores 0U.
  if (!(uadd_overflow_zero_plus_zero_result == 0U))
    goto ERROR;

  // __builtin_uadd_overflow(0U, 0U, &uadd_overflow_zero_plus_zero_result) returns 0.
  if (!(uadd_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_plus_zero_result;
  int uadd_overflow_unsigned_int_max_plus_zero_overflow;
  uadd_overflow_unsigned_int_max_plus_zero_overflow = __builtin_uadd_overflow(unsigned_int_max, 0U, &uadd_overflow_unsigned_int_max_plus_zero_result);

  // __builtin_uadd_overflow(4294967295U, 0U, &uadd_overflow_unsigned_int_max_plus_zero_result) stores 4294967295U.
  if (!(uadd_overflow_unsigned_int_max_plus_zero_result == unsigned_int_max))
    goto ERROR;

  // __builtin_uadd_overflow(4294967295U, 0U, &uadd_overflow_unsigned_int_max_plus_zero_result) returns 0.
  if (!(uadd_overflow_unsigned_int_max_plus_zero_overflow == 0))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_minus_one_plus_one_result;
  int uadd_overflow_unsigned_int_max_minus_one_plus_one_overflow;
  uadd_overflow_unsigned_int_max_minus_one_plus_one_overflow = __builtin_uadd_overflow(4294967294U, 1U, &uadd_overflow_unsigned_int_max_minus_one_plus_one_result);

  // __builtin_uadd_overflow(4294967295U - 1U, 1U, &uadd_overflow_unsigned_int_max_minus_one_plus_one_result) stores 4294967295U.
  if (!(uadd_overflow_unsigned_int_max_minus_one_plus_one_result == unsigned_int_max))
    goto ERROR;

  // __builtin_uadd_overflow(4294967295U - 1U, 1U, &uadd_overflow_unsigned_int_max_minus_one_plus_one_result) returns 0.
  if (!(uadd_overflow_unsigned_int_max_minus_one_plus_one_overflow == 0))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_plus_one_result;
  int uadd_overflow_unsigned_int_max_plus_one_overflow;
  uadd_overflow_unsigned_int_max_plus_one_overflow = __builtin_uadd_overflow(unsigned_int_max, 1U, &uadd_overflow_unsigned_int_max_plus_one_result);

  // __builtin_uadd_overflow(4294967295U, 1U, &uadd_overflow_unsigned_int_max_plus_one_result) stores 0U.
  if (!(uadd_overflow_unsigned_int_max_plus_one_result == 0U))
    goto ERROR;

  // __builtin_uadd_overflow(4294967295U, 1U, &uadd_overflow_unsigned_int_max_plus_one_result) returns 1.
  if (!(uadd_overflow_unsigned_int_max_plus_one_overflow == 1))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_plus_two_result;
  int uadd_overflow_unsigned_int_max_plus_two_overflow;
  uadd_overflow_unsigned_int_max_plus_two_overflow = __builtin_uadd_overflow(unsigned_int_max, 2U, &uadd_overflow_unsigned_int_max_plus_two_result);

  // __builtin_uadd_overflow(4294967295U, 2U, &uadd_overflow_unsigned_int_max_plus_two_result) stores 1U.
  if (!(uadd_overflow_unsigned_int_max_plus_two_result == 1U))
    goto ERROR;

  // __builtin_uadd_overflow(4294967295U, 2U, &uadd_overflow_unsigned_int_max_plus_two_result) returns 1.
  if (!(uadd_overflow_unsigned_int_max_plus_two_overflow == 1))
    goto ERROR;


  unsigned int uadd_overflow_unsigned_int_max_plus_unsigned_int_max_result;
  int uadd_overflow_unsigned_int_max_plus_unsigned_int_max_overflow;
  uadd_overflow_unsigned_int_max_plus_unsigned_int_max_overflow = __builtin_uadd_overflow(unsigned_int_max, unsigned_int_max, &uadd_overflow_unsigned_int_max_plus_unsigned_int_max_result);

  // __builtin_uadd_overflow(4294967295U, 4294967295U, &uadd_overflow_unsigned_int_max_plus_unsigned_int_max_result) stores 4294967295U -
  // 1U.
  if (!(uadd_overflow_unsigned_int_max_plus_unsigned_int_max_result == 4294967294U))
    goto ERROR;

  // __builtin_uadd_overflow(4294967295U, 4294967295U, &uadd_overflow_unsigned_int_max_plus_unsigned_int_max_result) returns 1.
  if (!(uadd_overflow_unsigned_int_max_plus_unsigned_int_max_overflow == 1))
    goto ERROR;


  // Tests for __builtin_uaddl_overflow.


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

  // __builtin_uaddl_overflow(0UL, 0UL, &uaddl_overflow_zero_plus_zero_result) stores 0UL.
  if (!(uaddl_overflow_zero_plus_zero_result == 0UL))
    goto ERROR;

  // __builtin_uaddl_overflow(0UL, 0UL, &uaddl_overflow_zero_plus_zero_result) returns 0.
  if (!(uaddl_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_plus_zero_result;
  int uaddl_overflow_unsigned_long_max_plus_zero_overflow;
  uaddl_overflow_unsigned_long_max_plus_zero_overflow = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 0UL, &uaddl_overflow_unsigned_long_max_plus_zero_result);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 0UL, &uaddl_overflow_unsigned_long_max_plus_zero_result) stores 4294967295UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 0UL, &uaddl_overflow_unsigned_long_max_plus_zero_result) stores
  // 18446744073709551615UL.
  if (!(uaddl_overflow_unsigned_long_max_plus_zero_result == uaddl_overflow_unsigned_long_max))
    goto ERROR;

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 0UL, &uaddl_overflow_unsigned_long_max_plus_zero_result) returns 0.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 0UL, &uaddl_overflow_unsigned_long_max_plus_zero_result) returns 0.
  if (!(uaddl_overflow_unsigned_long_max_plus_zero_overflow == 0))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_minus_one_plus_one_result;
  int uaddl_overflow_unsigned_long_max_minus_one_plus_one_overflow;
  uaddl_overflow_unsigned_long_max_minus_one_plus_one_overflow = __builtin_uaddl_overflow((~0UL) - 1UL, 1UL, &uaddl_overflow_unsigned_long_max_minus_one_plus_one_result);

  // ILP32: __builtin_uaddl_overflow(4294967294UL, 1UL, &uaddl_overflow_unsigned_long_max_minus_one_plus_one_result) stores 4294967295UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551614UL, 1UL, &uaddl_overflow_unsigned_long_max_minus_one_plus_one_result) stores
  // 18446744073709551615UL.
  if (!(uaddl_overflow_unsigned_long_max_minus_one_plus_one_result == uaddl_overflow_unsigned_long_max))
    goto ERROR;

  // ILP32: __builtin_uaddl_overflow(4294967294UL, 1UL, &uaddl_overflow_unsigned_long_max_minus_one_plus_one_result) returns 0.
  // LP64: __builtin_uaddl_overflow(18446744073709551614UL, 1UL, &uaddl_overflow_unsigned_long_max_minus_one_plus_one_result) returns 0.
  if (!(uaddl_overflow_unsigned_long_max_minus_one_plus_one_overflow == 0))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_plus_one_result;
  int uaddl_overflow_unsigned_long_max_plus_one_overflow;
  uaddl_overflow_unsigned_long_max_plus_one_overflow = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 1UL, &uaddl_overflow_unsigned_long_max_plus_one_result);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 1UL, &uaddl_overflow_unsigned_long_max_plus_one_result) stores 0UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 1UL, &uaddl_overflow_unsigned_long_max_plus_one_result) stores 0UL.
  if (!(uaddl_overflow_unsigned_long_max_plus_one_result == 0UL))
    goto ERROR;

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 1UL, &uaddl_overflow_unsigned_long_max_plus_one_result) returns 1.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 1UL, &uaddl_overflow_unsigned_long_max_plus_one_result) returns 1.
  if (!(uaddl_overflow_unsigned_long_max_plus_one_overflow == 1))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_plus_two_result;
  int uaddl_overflow_unsigned_long_max_plus_two_overflow;
  uaddl_overflow_unsigned_long_max_plus_two_overflow = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 2UL, &uaddl_overflow_unsigned_long_max_plus_two_result);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 2UL, &uaddl_overflow_unsigned_long_max_plus_two_result) stores 1UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 2UL, &uaddl_overflow_unsigned_long_max_plus_two_result) stores 1UL.
  if (!(uaddl_overflow_unsigned_long_max_plus_two_result == 1UL))
    goto ERROR;

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 2UL, &uaddl_overflow_unsigned_long_max_plus_two_result) returns 1.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 2UL, &uaddl_overflow_unsigned_long_max_plus_two_result) returns 1.
  if (!(uaddl_overflow_unsigned_long_max_plus_two_overflow == 1))
    goto ERROR;


  unsigned long int uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result;
  int uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_overflow;
  uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_overflow = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, uaddl_overflow_unsigned_long_max, &uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 4294967295UL, &uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result) stores
  // 4294967294UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 18446744073709551615UL,
  // &uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result) stores 18446744073709551614UL.
  if (!(uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result == (~0UL) - 1UL))
    goto ERROR;

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 4294967295UL, &uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result) returns 1.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 18446744073709551615UL,
  // &uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_result) returns 1.
  if (!(uaddl_overflow_unsigned_long_max_plus_unsigned_long_max_overflow == 1))
    goto ERROR;


  // Tests for __builtin_uaddll_overflow.

  unsigned long long int uaddll_overflow_zero_plus_zero_result;
  int uaddll_overflow_zero_plus_zero_overflow;
  uaddll_overflow_zero_plus_zero_overflow = __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_overflow_zero_plus_zero_result);

  // __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_overflow_zero_plus_zero_result) stores 0ULL.
  if (!(uaddll_overflow_zero_plus_zero_result == 0ULL))
    goto ERROR;

  // __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_overflow_zero_plus_zero_result) returns 0.
  if (!(uaddll_overflow_zero_plus_zero_overflow == 0))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_plus_zero_result;
  int uaddll_overflow_unsigned_long_long_max_plus_zero_overflow;
  uaddll_overflow_unsigned_long_long_max_plus_zero_overflow = __builtin_uaddll_overflow(unsigned_long_long_max, 0ULL, &uaddll_overflow_unsigned_long_long_max_plus_zero_result);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 0ULL, &uaddll_overflow_unsigned_long_long_max_plus_zero_result) stores
  // 18446744073709551615ULL.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_zero_result == unsigned_long_long_max))
    goto ERROR;

  // __builtin_uaddll_overflow(18446744073709551615ULL, 0ULL, &uaddll_overflow_unsigned_long_long_max_plus_zero_result) returns 0.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_zero_overflow == 0))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_result;
  int uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_overflow;
  uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_overflow = __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_result);

  // __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_result) stores
  // 18446744073709551615ULL.
  if (!(uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_result == unsigned_long_long_max))
    goto ERROR;

  // __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_result) returns 0.
  if (!(uaddll_overflow_unsigned_long_long_max_minus_one_plus_one_overflow == 0))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_plus_one_result;
  int uaddll_overflow_unsigned_long_long_max_plus_one_overflow;
  uaddll_overflow_unsigned_long_long_max_plus_one_overflow = __builtin_uaddll_overflow(unsigned_long_long_max, 1ULL, &uaddll_overflow_unsigned_long_long_max_plus_one_result);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 1ULL, &uaddll_overflow_unsigned_long_long_max_plus_one_result) stores 0ULL.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_one_result == 0ULL))
    goto ERROR;

  // __builtin_uaddll_overflow(18446744073709551615ULL, 1ULL, &uaddll_overflow_unsigned_long_long_max_plus_one_result) returns 1.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_one_overflow == 1))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_plus_two_result;
  int uaddll_overflow_unsigned_long_long_max_plus_two_overflow;
  uaddll_overflow_unsigned_long_long_max_plus_two_overflow = __builtin_uaddll_overflow(unsigned_long_long_max, 2ULL, &uaddll_overflow_unsigned_long_long_max_plus_two_result);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 2ULL, &uaddll_overflow_unsigned_long_long_max_plus_two_result) stores 1ULL.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_two_result == 1ULL))
    goto ERROR;

  // __builtin_uaddll_overflow(18446744073709551615ULL, 2ULL, &uaddll_overflow_unsigned_long_long_max_plus_two_result) returns 1.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_two_overflow == 1))
    goto ERROR;


  unsigned long long int uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_result;
  int uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_overflow;
  uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_overflow = __builtin_uaddll_overflow(unsigned_long_long_max, unsigned_long_long_max, &uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_result);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 18446744073709551615ULL,
  // &uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_result) stores 18446744073709551614ULL.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_result == 18446744073709551614ULL))
    goto ERROR;

  // __builtin_uaddll_overflow(18446744073709551615ULL, 18446744073709551615ULL,
  // &uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_result) returns 1.
  if (!(uaddll_overflow_unsigned_long_long_max_plus_unsigned_long_long_max_overflow == 1))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
