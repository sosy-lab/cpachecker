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
  const signed char signed_char_min = -128;
  const signed char signed_char_max = 127;
  const unsigned char unsigned_char_max = 255U;
  const short int short_max = 32767;
  const int int_min = (-2147483647 - 1);
  const int int_max = 2147483647;
  const unsigned int unsigned_int_max = 4294967295U;
  const long long int long_long_min = (-9223372036854775807LL - 1LL);
  const long long int long_long_max = 9223372036854775807LL;
  const unsigned long long int unsigned_long_long_max = 18446744073709551615ULL;
  const char char_minus_two = -2;
  const unsigned long int mul_overflow_unsigned_long_max = ~0UL;
  const long int mul_overflow_long_max = (long int)((~0UL) >> 1);
  const unsigned long int smull_overflow_unsigned_long_max = ~0UL;
  const long int smull_overflow_long_max = (long int)((~0UL) >> 1);
  const long int smull_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);
  const unsigned long int umull_overflow_unsigned_long_max = ~0UL;


  // This program targets ILP32 and fails its expected verdict under LP64.

  // ILP32: 2147483647L * 2L overflows long, stores -2L, and returns 1.
  long int model_smull_result;
  int model_smull_overflow;
  model_smull_overflow = __builtin_smull_overflow(2147483647L, 2L, &model_smull_result);

  if (!(model_smull_result != 0L && model_smull_overflow == 1))
    goto ERROR;

  // Tests for __builtin_mul_overflow.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)mul_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)mul_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  if (!((sizeof(long int) == sizeof(int) && (int)mul_overflow_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)mul_overflow_long_max == (long long int)(long int)long_long_max)))
    goto ERROR;


  signed char mul_overflow_signed_char_max_times_one_as_signed_char_result;
  int mul_overflow_signed_char_max_times_one_as_signed_char_overflow;
  mul_overflow_signed_char_max_times_one_as_signed_char_overflow = __builtin_mul_overflow(signed_char_max, 1, &mul_overflow_signed_char_max_times_one_as_signed_char_result);

  // __builtin_mul_overflow(127, 1, &mul_overflow_signed_char_max_times_one_as_signed_char_result) stores 127.
  if (!(mul_overflow_signed_char_max_times_one_as_signed_char_result == signed_char_max))
    goto ERROR;

  // __builtin_mul_overflow(127, 1, &mul_overflow_signed_char_max_times_one_as_signed_char_result) returns 0.
  if (!(mul_overflow_signed_char_max_times_one_as_signed_char_overflow == 0))
    goto ERROR;


  signed char mul_overflow_signed_char_min_times_minus_one_as_signed_char_result;
  int mul_overflow_signed_char_min_times_minus_one_as_signed_char_overflow;
  mul_overflow_signed_char_min_times_minus_one_as_signed_char_overflow = __builtin_mul_overflow(signed_char_min, -1, &mul_overflow_signed_char_min_times_minus_one_as_signed_char_result);

  // __builtin_mul_overflow(-128, -1, &mul_overflow_signed_char_min_times_minus_one_as_signed_char_result) stores -128.
  if (!(mul_overflow_signed_char_min_times_minus_one_as_signed_char_result == signed_char_min))
    goto ERROR;

  // __builtin_mul_overflow(-128, -1, &mul_overflow_signed_char_min_times_minus_one_as_signed_char_result) returns 1.
  if (!(mul_overflow_signed_char_min_times_minus_one_as_signed_char_overflow == 1))
    goto ERROR;


  unsigned char mul_overflow_unsigned_char_max_times_unsigned_char_max_as_unsigned_char_result;
  int mul_overflow_unsigned_char_max_times_unsigned_char_max_as_unsigned_char_overflow;
  mul_overflow_unsigned_char_max_times_unsigned_char_max_as_unsigned_char_overflow = __builtin_mul_overflow(unsigned_char_max, unsigned_char_max, &mul_overflow_unsigned_char_max_times_unsigned_char_max_as_unsigned_char_result);

  // __builtin_mul_overflow(255U, 255U, &mul_overflow_unsigned_char_max_times_unsigned_char_max_as_unsigned_char_result) stores 1U.
  if (!(mul_overflow_unsigned_char_max_times_unsigned_char_max_as_unsigned_char_result == 1U))
    goto ERROR;

  // __builtin_mul_overflow(255U, 255U, &mul_overflow_unsigned_char_max_times_unsigned_char_max_as_unsigned_char_result) returns 1.
  if (!(mul_overflow_unsigned_char_max_times_unsigned_char_max_as_unsigned_char_overflow == 1))
    goto ERROR;


  short int mul_overflow_short_max_times_two_as_short_int_result;
  int mul_overflow_short_max_times_two_as_short_int_overflow;
  mul_overflow_short_max_times_two_as_short_int_overflow = __builtin_mul_overflow(short_max, 2, &mul_overflow_short_max_times_two_as_short_int_result);

  // __builtin_mul_overflow(32767, 2, &mul_overflow_short_max_times_two_as_short_int_result) stores -2.
  if (!(mul_overflow_short_max_times_two_as_short_int_result == -2))
    goto ERROR;

  // __builtin_mul_overflow(32767, 2, &mul_overflow_short_max_times_two_as_short_int_result) returns 1.
  if (!(mul_overflow_short_max_times_two_as_short_int_overflow == 1))
    goto ERROR;


  unsigned short int mul_overflow_two_times_32767_as_unsigned_short_int_result;
  int mul_overflow_two_times_32767_as_unsigned_short_int_overflow;
  mul_overflow_two_times_32767_as_unsigned_short_int_overflow = __builtin_mul_overflow(2U, 32767U, &mul_overflow_two_times_32767_as_unsigned_short_int_result);

  // __builtin_mul_overflow(2U, 65535U / 2U, &mul_overflow_two_times_32767_as_unsigned_short_int_result) stores 65535U - 1U.
  if (!(mul_overflow_two_times_32767_as_unsigned_short_int_result == 65534U))
    goto ERROR;

  // __builtin_mul_overflow(2U, 65535U / 2U, &mul_overflow_two_times_32767_as_unsigned_short_int_result) returns 0.
  if (!(mul_overflow_two_times_32767_as_unsigned_short_int_overflow == 0))
    goto ERROR;


  int mul_overflow_int_min_times_minus_one_as_int_result;
  int mul_overflow_int_min_times_minus_one_as_int_overflow;
  mul_overflow_int_min_times_minus_one_as_int_overflow = __builtin_mul_overflow(int_min, -1, &mul_overflow_int_min_times_minus_one_as_int_result);

  // __builtin_mul_overflow((-2147483647 - 1), -1, &mul_overflow_int_min_times_minus_one_as_int_result) stores (-2147483647 - 1).
  if (!(mul_overflow_int_min_times_minus_one_as_int_result == int_min))
    goto ERROR;

  // __builtin_mul_overflow((-2147483647 - 1), -1, &mul_overflow_int_min_times_minus_one_as_int_result) returns 1.
  if (!(mul_overflow_int_min_times_minus_one_as_int_overflow == 1))
    goto ERROR;


  unsigned int mul_overflow_unsigned_int_max_times_unsigned_int_max_as_unsigned_int_result;
  int mul_overflow_unsigned_int_max_times_unsigned_int_max_as_unsigned_int_overflow;
  mul_overflow_unsigned_int_max_times_unsigned_int_max_as_unsigned_int_overflow = __builtin_mul_overflow(unsigned_int_max, unsigned_int_max, &mul_overflow_unsigned_int_max_times_unsigned_int_max_as_unsigned_int_result);

  // __builtin_mul_overflow(4294967295U, 4294967295U, &mul_overflow_unsigned_int_max_times_unsigned_int_max_as_unsigned_int_result) stores
  // 1U.
  if (!(mul_overflow_unsigned_int_max_times_unsigned_int_max_as_unsigned_int_result == 1U))
    goto ERROR;

  // __builtin_mul_overflow(4294967295U, 4294967295U, &mul_overflow_unsigned_int_max_times_unsigned_int_max_as_unsigned_int_result) returns
  // 1.
  if (!(mul_overflow_unsigned_int_max_times_unsigned_int_max_as_unsigned_int_overflow == 1))
    goto ERROR;


  long int mul_overflow_long_max_times_zero_as_long_int_result;
  int mul_overflow_long_max_times_zero_as_long_int_overflow;
  mul_overflow_long_max_times_zero_as_long_int_overflow = __builtin_mul_overflow(mul_overflow_long_max, 0L, &mul_overflow_long_max_times_zero_as_long_int_result);

  // ILP32: __builtin_mul_overflow(2147483647L, 0L, &mul_overflow_long_max_times_zero_as_long_int_result) stores 0L.
  // LP64: __builtin_mul_overflow(9223372036854775807L, 0L, &mul_overflow_long_max_times_zero_as_long_int_result) stores 0L.
  if (!(mul_overflow_long_max_times_zero_as_long_int_result == 0L))
    goto ERROR;

  // ILP32: __builtin_mul_overflow(2147483647L, 0L, &mul_overflow_long_max_times_zero_as_long_int_result) returns 0.
  // LP64: __builtin_mul_overflow(9223372036854775807L, 0L, &mul_overflow_long_max_times_zero_as_long_int_result) returns 0.
  if (!(mul_overflow_long_max_times_zero_as_long_int_overflow == 0))
    goto ERROR;


  unsigned long int mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_result;
  int mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_overflow;
  mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_overflow = __builtin_mul_overflow(mul_overflow_unsigned_long_max, 2UL, &mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_result);

  // ILP32: __builtin_mul_overflow(4294967295UL, 2UL, &mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_result) stores
  // 4294967294UL.
  // LP64: __builtin_mul_overflow(18446744073709551615UL, 2UL, &mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_result) stores
  // 18446744073709551614UL.
  if (!(mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_result == (~0UL) - 1UL))
    goto ERROR;

  // ILP32: __builtin_mul_overflow(4294967295UL, 2UL, &mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_result) returns 1.
  // LP64: __builtin_mul_overflow(18446744073709551615UL, 2UL, &mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_result)
  // returns 1.
  if (!(mul_overflow_unsigned_long_max_times_two_as_unsigned_long_int_overflow == 1))
    goto ERROR;


  long long int mul_overflow_long_long_min_times_zero_as_long_long_int_result;
  int mul_overflow_long_long_min_times_zero_as_long_long_int_overflow;
  mul_overflow_long_long_min_times_zero_as_long_long_int_overflow = __builtin_mul_overflow(long_long_min, 0LL, &mul_overflow_long_long_min_times_zero_as_long_long_int_result);

  // __builtin_mul_overflow(-9223372036854775808LL, 0LL, &mul_overflow_long_long_min_times_zero_as_long_long_int_result) stores 0LL.
  if (!(mul_overflow_long_long_min_times_zero_as_long_long_int_result == 0LL))
    goto ERROR;

  // __builtin_mul_overflow(-9223372036854775808LL, 0LL, &mul_overflow_long_long_min_times_zero_as_long_long_int_result) returns 0.
  if (!(mul_overflow_long_long_min_times_zero_as_long_long_int_overflow == 0))
    goto ERROR;


  unsigned long long int mul_overflow_unsigned_long_long_max_times_unsigned_long_long_max_as_unsigned_long_long_int_result;
  int mul_overflow_unsigned_long_long_max_times_unsigned_long_long_max_as_unsigned_long_long_int_overflow;
  mul_overflow_unsigned_long_long_max_times_unsigned_long_long_max_as_unsigned_long_long_int_overflow = __builtin_mul_overflow(unsigned_long_long_max, unsigned_long_long_max, &mul_overflow_unsigned_long_long_max_times_unsigned_long_long_max_as_unsigned_long_long_int_result);

  // __builtin_mul_overflow(18446744073709551615ULL, 18446744073709551615ULL,
  // &mul_overflow_unsigned_long_long_max_times_unsigned_long_long_max_as_unsigned_long_long_int_result) stores 1ULL.
  if (!(mul_overflow_unsigned_long_long_max_times_unsigned_long_long_max_as_unsigned_long_long_int_result == 1ULL))
    goto ERROR;

  // __builtin_mul_overflow(18446744073709551615ULL, 18446744073709551615ULL,
  // &mul_overflow_unsigned_long_long_max_times_unsigned_long_long_max_as_unsigned_long_long_int_result) returns 1.
  if (!(mul_overflow_unsigned_long_long_max_times_unsigned_long_long_max_as_unsigned_long_long_int_overflow == 1))
    goto ERROR;


  char mul_overflow_char_max_times_two_as_char_result;
  int mul_overflow_char_max_times_two_as_char_overflow;
  mul_overflow_char_max_times_two_as_char_overflow = __builtin_mul_overflow(char_max, 2, &mul_overflow_char_max_times_two_as_char_result);

  // __builtin_mul_overflow((char)127, 2, &mul_overflow_char_max_times_two_as_char_result) stores (char)-2.
  if (!(mul_overflow_char_max_times_two_as_char_result == char_minus_two))
    goto ERROR;

  // __builtin_mul_overflow(127, 2, &mul_overflow_char_max_times_two_as_char_result) or __builtin_mul_overflow(255, 2,
  // &mul_overflow_char_max_times_two_as_char_result)
  // returns 1, according to plain-char signedness.
  if (!(mul_overflow_char_max_times_two_as_char_overflow == 1))
    goto ERROR;


  int mul_overflow_minus_one_times_one_as_int_result;
  int mul_overflow_minus_one_times_one_as_int_overflow;
  mul_overflow_minus_one_times_one_as_int_overflow = __builtin_mul_overflow(-1LL, 1ULL, &mul_overflow_minus_one_times_one_as_int_result);

  // __builtin_mul_overflow(-1LL, 1ULL, &mul_overflow_minus_one_times_one_as_int_result) stores -1.
  if (!(mul_overflow_minus_one_times_one_as_int_result == -1))
    goto ERROR;

  // __builtin_mul_overflow(-1LL, 1ULL, &mul_overflow_minus_one_times_one_as_int_result) returns 0.
  if (!(mul_overflow_minus_one_times_one_as_int_overflow == 0))
    goto ERROR;


  unsigned int mul_overflow_minus_one_times_two_as_unsigned_int_result;
  int mul_overflow_minus_one_times_two_as_unsigned_int_overflow;
  mul_overflow_minus_one_times_two_as_unsigned_int_overflow = __builtin_mul_overflow(-1LL, 2ULL, &mul_overflow_minus_one_times_two_as_unsigned_int_result);

  // __builtin_mul_overflow(-1LL, 2ULL, &mul_overflow_minus_one_times_two_as_unsigned_int_result) stores 4294967295U - 1U.
  if (!(mul_overflow_minus_one_times_two_as_unsigned_int_result == 4294967294U))
    goto ERROR;

  // __builtin_mul_overflow(-1LL, 2ULL, &mul_overflow_minus_one_times_two_as_unsigned_int_result) returns 1.
  if (!(mul_overflow_minus_one_times_two_as_unsigned_int_overflow == 1))
    goto ERROR;


  // Tests for __builtin_smul_overflow.

  int smul_overflow_zero_times_zero_result;
  int smul_overflow_zero_times_zero_overflow;
  smul_overflow_zero_times_zero_overflow = __builtin_smul_overflow(0, 0, &smul_overflow_zero_times_zero_result);

  // __builtin_smul_overflow(0, 0, &smul_overflow_zero_times_zero_result) stores 0.
  if (!(smul_overflow_zero_times_zero_result == 0))
    goto ERROR;

  // __builtin_smul_overflow(0, 0, &smul_overflow_zero_times_zero_result) returns 0.
  if (!(smul_overflow_zero_times_zero_overflow == 0))
    goto ERROR;


  int smul_overflow_int_max_times_zero_result;
  int smul_overflow_int_max_times_zero_overflow;
  smul_overflow_int_max_times_zero_overflow = __builtin_smul_overflow(int_max, 0, &smul_overflow_int_max_times_zero_result);

  // __builtin_smul_overflow(2147483647, 0, &smul_overflow_int_max_times_zero_result) stores 0.
  if (!(smul_overflow_int_max_times_zero_result == 0))
    goto ERROR;

  // __builtin_smul_overflow(2147483647, 0, &smul_overflow_int_max_times_zero_result) returns 0.
  if (!(smul_overflow_int_max_times_zero_overflow == 0))
    goto ERROR;


  int smul_overflow_int_min_times_zero_result;
  int smul_overflow_int_min_times_zero_overflow;
  smul_overflow_int_min_times_zero_overflow = __builtin_smul_overflow(int_min, 0, &smul_overflow_int_min_times_zero_result);

  // __builtin_smul_overflow((-2147483647 - 1), 0, &smul_overflow_int_min_times_zero_result) stores 0.
  if (!(smul_overflow_int_min_times_zero_result == 0))
    goto ERROR;

  // __builtin_smul_overflow((-2147483647 - 1), 0, &smul_overflow_int_min_times_zero_result) returns 0.
  if (!(smul_overflow_int_min_times_zero_overflow == 0))
    goto ERROR;


  int smul_overflow_int_max_times_one_result;
  int smul_overflow_int_max_times_one_overflow;
  smul_overflow_int_max_times_one_overflow = __builtin_smul_overflow(int_max, 1, &smul_overflow_int_max_times_one_result);

  // __builtin_smul_overflow(2147483647, 1, &smul_overflow_int_max_times_one_result) stores 2147483647.
  if (!(smul_overflow_int_max_times_one_result == int_max))
    goto ERROR;

  // __builtin_smul_overflow(2147483647, 1, &smul_overflow_int_max_times_one_result) returns 0.
  if (!(smul_overflow_int_max_times_one_overflow == 0))
    goto ERROR;


  int smul_overflow_int_min_times_one_result;
  int smul_overflow_int_min_times_one_overflow;
  smul_overflow_int_min_times_one_overflow = __builtin_smul_overflow(int_min, 1, &smul_overflow_int_min_times_one_result);

  // __builtin_smul_overflow((-2147483647 - 1), 1, &smul_overflow_int_min_times_one_result) stores (-2147483647 - 1).
  if (!(smul_overflow_int_min_times_one_result == int_min))
    goto ERROR;

  // __builtin_smul_overflow((-2147483647 - 1), 1, &smul_overflow_int_min_times_one_result) returns 0.
  if (!(smul_overflow_int_min_times_one_overflow == 0))
    goto ERROR;


  int smul_overflow_int_max_times_minus_one_result;
  int smul_overflow_int_max_times_minus_one_overflow;
  smul_overflow_int_max_times_minus_one_overflow = __builtin_smul_overflow(int_max, -1, &smul_overflow_int_max_times_minus_one_result);

  // __builtin_smul_overflow(2147483647, -1, &smul_overflow_int_max_times_minus_one_result) stores -2147483647.
  if (!(smul_overflow_int_max_times_minus_one_result == -int_max))
    goto ERROR;

  // __builtin_smul_overflow(2147483647, -1, &smul_overflow_int_max_times_minus_one_result) returns 0.
  if (!(smul_overflow_int_max_times_minus_one_overflow == 0))
    goto ERROR;


  int smul_overflow_int_min_times_minus_one_result;
  int smul_overflow_int_min_times_minus_one_overflow;
  smul_overflow_int_min_times_minus_one_overflow = __builtin_smul_overflow(int_min, -1, &smul_overflow_int_min_times_minus_one_result);

  // __builtin_smul_overflow((-2147483647 - 1), -1, &smul_overflow_int_min_times_minus_one_result) stores (-2147483647 - 1).
  if (!(smul_overflow_int_min_times_minus_one_result == int_min))
    goto ERROR;

  // __builtin_smul_overflow((-2147483647 - 1), -1, &smul_overflow_int_min_times_minus_one_result) returns 1.
  if (!(smul_overflow_int_min_times_minus_one_overflow == 1))
    goto ERROR;


  int smul_overflow_int_max_times_two_result;
  int smul_overflow_int_max_times_two_overflow;
  smul_overflow_int_max_times_two_overflow = __builtin_smul_overflow(int_max, 2, &smul_overflow_int_max_times_two_result);

  // __builtin_smul_overflow(2147483647, 2, &smul_overflow_int_max_times_two_result) stores -2.
  if (!(smul_overflow_int_max_times_two_result == -2))
    goto ERROR;

  // __builtin_smul_overflow(2147483647, 2, &smul_overflow_int_max_times_two_result) returns 1.
  if (!(smul_overflow_int_max_times_two_overflow == 1))
    goto ERROR;


  int smul_overflow_int_min_times_two_result;
  int smul_overflow_int_min_times_two_overflow;
  smul_overflow_int_min_times_two_overflow = __builtin_smul_overflow(int_min, 2, &smul_overflow_int_min_times_two_result);

  // __builtin_smul_overflow((-2147483647 - 1), 2, &smul_overflow_int_min_times_two_result) stores 0.
  if (!(smul_overflow_int_min_times_two_result == 0))
    goto ERROR;

  // __builtin_smul_overflow((-2147483647 - 1), 2, &smul_overflow_int_min_times_two_result) returns 1.
  if (!(smul_overflow_int_min_times_two_overflow == 1))
    goto ERROR;


  int smul_overflow_minus_one_times_minus_one_result;
  int smul_overflow_minus_one_times_minus_one_overflow;
  smul_overflow_minus_one_times_minus_one_overflow = __builtin_smul_overflow(-1, -1, &smul_overflow_minus_one_times_minus_one_result);

  // __builtin_smul_overflow(-1, -1, &smul_overflow_minus_one_times_minus_one_result) stores 1.
  if (!(smul_overflow_minus_one_times_minus_one_result == 1))
    goto ERROR;

  // __builtin_smul_overflow(-1, -1, &smul_overflow_minus_one_times_minus_one_result) returns 0.
  if (!(smul_overflow_minus_one_times_minus_one_overflow == 0))
    goto ERROR;


  // Tests for __builtin_smull_overflow.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)smull_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)smull_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  if (!((sizeof(long int) == sizeof(int) && (int)smull_overflow_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)smull_overflow_long_max == (long long int)(long int)long_long_max)))
    goto ERROR;


  // ILP32: The calculated long minimum must equal int minimum -2147483648.
  // LP64: The calculated long minimum must equal long long minimum -9223372036854775808.
  if (!((sizeof(long int) == sizeof(int) && (int)smull_overflow_long_min == int_min) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)smull_overflow_long_min ==
            (long long int)(long int)long_long_min)))
    goto ERROR;


  long int smull_overflow_zero_times_zero_result;
  int smull_overflow_zero_times_zero_overflow;
  smull_overflow_zero_times_zero_overflow = __builtin_smull_overflow(0L, 0L, &smull_overflow_zero_times_zero_result);

  // __builtin_smull_overflow(0L, 0L, &smull_overflow_zero_times_zero_result) stores 0L.
  if (!(smull_overflow_zero_times_zero_result == 0L))
    goto ERROR;

  // __builtin_smull_overflow(0L, 0L, &smull_overflow_zero_times_zero_result) returns 0.
  if (!(smull_overflow_zero_times_zero_overflow == 0))
    goto ERROR;


  long int smull_overflow_long_max_times_zero_result;
  int smull_overflow_long_max_times_zero_overflow;
  smull_overflow_long_max_times_zero_overflow = __builtin_smull_overflow(smull_overflow_long_max, 0L, &smull_overflow_long_max_times_zero_result);

  // ILP32: __builtin_smull_overflow(2147483647L, 0L, &smull_overflow_long_max_times_zero_result) stores 0L.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 0L, &smull_overflow_long_max_times_zero_result) stores 0L.
  if (!(smull_overflow_long_max_times_zero_result == 0L))
    goto ERROR;

  // ILP32: __builtin_smull_overflow(2147483647L, 0L, &smull_overflow_long_max_times_zero_result) returns 0.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 0L, &smull_overflow_long_max_times_zero_result) returns 0.
  if (!(smull_overflow_long_max_times_zero_overflow == 0))
    goto ERROR;


  long int smull_overflow_long_min_times_zero_result;
  int smull_overflow_long_min_times_zero_overflow;
  smull_overflow_long_min_times_zero_overflow = __builtin_smull_overflow(smull_overflow_long_min, 0L, &smull_overflow_long_min_times_zero_result);

  // ILP32: __builtin_smull_overflow(-2147483648L, 0L, &smull_overflow_long_min_times_zero_result) stores 0L.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 0L, &smull_overflow_long_min_times_zero_result) stores 0L.
  if (!(smull_overflow_long_min_times_zero_result == 0L))
    goto ERROR;

  // ILP32: __builtin_smull_overflow(-2147483648L, 0L, &smull_overflow_long_min_times_zero_result) returns 0.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 0L, &smull_overflow_long_min_times_zero_result) returns 0.
  if (!(smull_overflow_long_min_times_zero_overflow == 0))
    goto ERROR;


  long int smull_overflow_long_max_times_one_result;
  int smull_overflow_long_max_times_one_overflow;
  smull_overflow_long_max_times_one_overflow = __builtin_smull_overflow(smull_overflow_long_max, 1L, &smull_overflow_long_max_times_one_result);

  // ILP32: __builtin_smull_overflow(2147483647L, 1L, &smull_overflow_long_max_times_one_result) stores 2147483647L.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 1L, &smull_overflow_long_max_times_one_result) stores 9223372036854775807L.
  if (!(smull_overflow_long_max_times_one_result == smull_overflow_long_max))
    goto ERROR;

  // ILP32: __builtin_smull_overflow(2147483647L, 1L, &smull_overflow_long_max_times_one_result) returns 0.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 1L, &smull_overflow_long_max_times_one_result) returns 0.
  if (!(smull_overflow_long_max_times_one_overflow == 0))
    goto ERROR;


  long int smull_overflow_long_min_times_one_result;
  int smull_overflow_long_min_times_one_overflow;
  smull_overflow_long_min_times_one_overflow = __builtin_smull_overflow(smull_overflow_long_min, 1L, &smull_overflow_long_min_times_one_result);

  // ILP32: __builtin_smull_overflow(-2147483648L, 1L, &smull_overflow_long_min_times_one_result) stores -2147483648L.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 1L, &smull_overflow_long_min_times_one_result) stores -9223372036854775808L.
  if (!(smull_overflow_long_min_times_one_result == smull_overflow_long_min))
    goto ERROR;

  // ILP32: __builtin_smull_overflow(-2147483648L, 1L, &smull_overflow_long_min_times_one_result) returns 0.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 1L, &smull_overflow_long_min_times_one_result) returns 0.
  if (!(smull_overflow_long_min_times_one_overflow == 0))
    goto ERROR;


  long int smull_overflow_long_max_times_minus_one_result;
  int smull_overflow_long_max_times_minus_one_overflow;
  smull_overflow_long_max_times_minus_one_overflow = __builtin_smull_overflow(smull_overflow_long_max, -1L, &smull_overflow_long_max_times_minus_one_result);

  // ILP32: __builtin_smull_overflow(2147483647L, -1L, &smull_overflow_long_max_times_minus_one_result) stores -2147483647L.
  // LP64: __builtin_smull_overflow(9223372036854775807L, -1L, &smull_overflow_long_max_times_minus_one_result) stores
  // -9223372036854775807L.
  if (!(smull_overflow_long_max_times_minus_one_result == - ((long int)((~0UL) >> 1))))
    goto ERROR;

  // ILP32: __builtin_smull_overflow(2147483647L, -1L, &smull_overflow_long_max_times_minus_one_result) returns 0.
  // LP64: __builtin_smull_overflow(9223372036854775807L, -1L, &smull_overflow_long_max_times_minus_one_result) returns 0.
  if (!(smull_overflow_long_max_times_minus_one_overflow == 0))
    goto ERROR;


  long int smull_overflow_long_min_times_minus_one_result;
  int smull_overflow_long_min_times_minus_one_overflow;
  smull_overflow_long_min_times_minus_one_overflow = __builtin_smull_overflow(smull_overflow_long_min, -1L, &smull_overflow_long_min_times_minus_one_result);

  // ILP32: __builtin_smull_overflow(-2147483648L, -1L, &smull_overflow_long_min_times_minus_one_result) stores -2147483648L.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, -1L, &smull_overflow_long_min_times_minus_one_result) stores
  // -9223372036854775808L.
  if (!(smull_overflow_long_min_times_minus_one_result == smull_overflow_long_min))
    goto ERROR;

  // ILP32: __builtin_smull_overflow(-2147483648L, -1L, &smull_overflow_long_min_times_minus_one_result) returns 1.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, -1L, &smull_overflow_long_min_times_minus_one_result) returns 1.
  if (!(smull_overflow_long_min_times_minus_one_overflow == 1))
    goto ERROR;


  long int smull_overflow_long_max_times_two_result;
  int smull_overflow_long_max_times_two_overflow;
  smull_overflow_long_max_times_two_overflow = __builtin_smull_overflow(smull_overflow_long_max, 2L, &smull_overflow_long_max_times_two_result);

  // ILP32: __builtin_smull_overflow(2147483647L, 2L, &smull_overflow_long_max_times_two_result) stores -2L.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 2L, &smull_overflow_long_max_times_two_result) stores -2L.
  if (!(smull_overflow_long_max_times_two_result == -2L))
    goto ERROR;

  // ILP32: __builtin_smull_overflow(2147483647L, 2L, &smull_overflow_long_max_times_two_result) returns 1.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 2L, &smull_overflow_long_max_times_two_result) returns 1.
  if (!(smull_overflow_long_max_times_two_overflow == 1))
    goto ERROR;


  long int smull_overflow_long_min_times_two_result;
  int smull_overflow_long_min_times_two_overflow;
  smull_overflow_long_min_times_two_overflow = __builtin_smull_overflow(smull_overflow_long_min, 2L, &smull_overflow_long_min_times_two_result);

  // ILP32: __builtin_smull_overflow(-2147483648L, 2L, &smull_overflow_long_min_times_two_result) stores 0L.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 2L, &smull_overflow_long_min_times_two_result) stores 0L.
  if (!(smull_overflow_long_min_times_two_result == 0L))
    goto ERROR;

  // ILP32: __builtin_smull_overflow(-2147483648L, 2L, &smull_overflow_long_min_times_two_result) returns 1.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 2L, &smull_overflow_long_min_times_two_result) returns 1.
  if (!(smull_overflow_long_min_times_two_overflow == 1))
    goto ERROR;


  long int smull_overflow_minus_one_times_minus_one_result;
  int smull_overflow_minus_one_times_minus_one_overflow;
  smull_overflow_minus_one_times_minus_one_overflow = __builtin_smull_overflow(-1L, -1L, &smull_overflow_minus_one_times_minus_one_result);

  // __builtin_smull_overflow(-1L, -1L, &smull_overflow_minus_one_times_minus_one_result) stores 1L.
  if (!(smull_overflow_minus_one_times_minus_one_result == 1L))
    goto ERROR;

  // __builtin_smull_overflow(-1L, -1L, &smull_overflow_minus_one_times_minus_one_result) returns 0.
  if (!(smull_overflow_minus_one_times_minus_one_overflow == 0))
    goto ERROR;


  // Tests for __builtin_smulll_overflow.

  long long int smulll_overflow_zero_times_zero_result;
  int smulll_overflow_zero_times_zero_overflow;
  smulll_overflow_zero_times_zero_overflow = __builtin_smulll_overflow(0LL, 0LL, &smulll_overflow_zero_times_zero_result);

  // __builtin_smulll_overflow(0LL, 0LL, &smulll_overflow_zero_times_zero_result) stores 0LL.
  if (!(smulll_overflow_zero_times_zero_result == 0LL))
    goto ERROR;

  // __builtin_smulll_overflow(0LL, 0LL, &smulll_overflow_zero_times_zero_result) returns 0.
  if (!(smulll_overflow_zero_times_zero_overflow == 0))
    goto ERROR;


  long long int smulll_overflow_long_long_max_times_zero_result;
  int smulll_overflow_long_long_max_times_zero_overflow;
  smulll_overflow_long_long_max_times_zero_overflow = __builtin_smulll_overflow(long_long_max, 0LL, &smulll_overflow_long_long_max_times_zero_result);

  // __builtin_smulll_overflow(9223372036854775807LL, 0LL, &smulll_overflow_long_long_max_times_zero_result) stores 0LL.
  if (!(smulll_overflow_long_long_max_times_zero_result == 0LL))
    goto ERROR;

  // __builtin_smulll_overflow(9223372036854775807LL, 0LL, &smulll_overflow_long_long_max_times_zero_result) returns 0.
  if (!(smulll_overflow_long_long_max_times_zero_overflow == 0))
    goto ERROR;


  long long int smulll_overflow_long_long_min_times_zero_result;
  int smulll_overflow_long_long_min_times_zero_overflow;
  smulll_overflow_long_long_min_times_zero_overflow = __builtin_smulll_overflow(long_long_min, 0LL, &smulll_overflow_long_long_min_times_zero_result);

  // __builtin_smulll_overflow(-9223372036854775808LL, 0LL, &smulll_overflow_long_long_min_times_zero_result) stores 0LL.
  if (!(smulll_overflow_long_long_min_times_zero_result == 0LL))
    goto ERROR;

  // __builtin_smulll_overflow(-9223372036854775808LL, 0LL, &smulll_overflow_long_long_min_times_zero_result) returns 0.
  if (!(smulll_overflow_long_long_min_times_zero_overflow == 0))
    goto ERROR;


  long long int smulll_overflow_long_long_max_times_one_result;
  int smulll_overflow_long_long_max_times_one_overflow;
  smulll_overflow_long_long_max_times_one_overflow = __builtin_smulll_overflow(long_long_max, 1LL, &smulll_overflow_long_long_max_times_one_result);

  // __builtin_smulll_overflow(9223372036854775807LL, 1LL, &smulll_overflow_long_long_max_times_one_result) stores 9223372036854775807LL.
  if (!(smulll_overflow_long_long_max_times_one_result == long_long_max))
    goto ERROR;

  // __builtin_smulll_overflow(9223372036854775807LL, 1LL, &smulll_overflow_long_long_max_times_one_result) returns 0.
  if (!(smulll_overflow_long_long_max_times_one_overflow == 0))
    goto ERROR;


  long long int smulll_overflow_long_long_min_times_one_result;
  int smulll_overflow_long_long_min_times_one_overflow;
  smulll_overflow_long_long_min_times_one_overflow = __builtin_smulll_overflow(long_long_min, 1LL, &smulll_overflow_long_long_min_times_one_result);

  // __builtin_smulll_overflow(-9223372036854775808LL, 1LL, &smulll_overflow_long_long_min_times_one_result) stores -9223372036854775808LL.
  if (!(smulll_overflow_long_long_min_times_one_result == long_long_min))
    goto ERROR;

  // __builtin_smulll_overflow(-9223372036854775808LL, 1LL, &smulll_overflow_long_long_min_times_one_result) returns 0.
  if (!(smulll_overflow_long_long_min_times_one_overflow == 0))
    goto ERROR;


  long long int smulll_overflow_long_long_max_times_minus_one_result;
  int smulll_overflow_long_long_max_times_minus_one_overflow;
  smulll_overflow_long_long_max_times_minus_one_overflow = __builtin_smulll_overflow(long_long_max, -1LL, &smulll_overflow_long_long_max_times_minus_one_result);

  // __builtin_smulll_overflow(9223372036854775807LL, -1LL, &smulll_overflow_long_long_max_times_minus_one_result) stores
  // -9223372036854775807LL.
  if (!(smulll_overflow_long_long_max_times_minus_one_result == -long_long_max))
    goto ERROR;

  // __builtin_smulll_overflow(9223372036854775807LL, -1LL, &smulll_overflow_long_long_max_times_minus_one_result) returns 0.
  if (!(smulll_overflow_long_long_max_times_minus_one_overflow == 0))
    goto ERROR;


  long long int smulll_overflow_long_long_min_times_minus_one_result;
  int smulll_overflow_long_long_min_times_minus_one_overflow;
  smulll_overflow_long_long_min_times_minus_one_overflow = __builtin_smulll_overflow(long_long_min, -1LL, &smulll_overflow_long_long_min_times_minus_one_result);

  // __builtin_smulll_overflow(-9223372036854775808LL, -1LL, &smulll_overflow_long_long_min_times_minus_one_result) stores
  // -9223372036854775808LL.
  if (!(smulll_overflow_long_long_min_times_minus_one_result == long_long_min))
    goto ERROR;

  // __builtin_smulll_overflow(-9223372036854775808LL, -1LL, &smulll_overflow_long_long_min_times_minus_one_result) returns 1.
  if (!(smulll_overflow_long_long_min_times_minus_one_overflow == 1))
    goto ERROR;


  long long int smulll_overflow_long_long_max_times_two_result;
  int smulll_overflow_long_long_max_times_two_overflow;
  smulll_overflow_long_long_max_times_two_overflow = __builtin_smulll_overflow(long_long_max, 2LL, &smulll_overflow_long_long_max_times_two_result);

  // __builtin_smulll_overflow(9223372036854775807LL, 2LL, &smulll_overflow_long_long_max_times_two_result) stores -2LL.
  if (!(smulll_overflow_long_long_max_times_two_result == -2LL))
    goto ERROR;

  // __builtin_smulll_overflow(9223372036854775807LL, 2LL, &smulll_overflow_long_long_max_times_two_result) returns 1.
  if (!(smulll_overflow_long_long_max_times_two_overflow == 1))
    goto ERROR;


  long long int smulll_overflow_long_long_min_times_two_result;
  int smulll_overflow_long_long_min_times_two_overflow;
  smulll_overflow_long_long_min_times_two_overflow = __builtin_smulll_overflow(long_long_min, 2LL, &smulll_overflow_long_long_min_times_two_result);

  // __builtin_smulll_overflow(-9223372036854775808LL, 2LL, &smulll_overflow_long_long_min_times_two_result) stores 0LL.
  if (!(smulll_overflow_long_long_min_times_two_result == 0LL))
    goto ERROR;

  // __builtin_smulll_overflow(-9223372036854775808LL, 2LL, &smulll_overflow_long_long_min_times_two_result) returns 1.
  if (!(smulll_overflow_long_long_min_times_two_overflow == 1))
    goto ERROR;


  long long int smulll_overflow_minus_one_times_minus_one_result;
  int smulll_overflow_minus_one_times_minus_one_overflow;
  smulll_overflow_minus_one_times_minus_one_overflow = __builtin_smulll_overflow(-1LL, -1LL, &smulll_overflow_minus_one_times_minus_one_result);

  // __builtin_smulll_overflow(-1LL, -1LL, &smulll_overflow_minus_one_times_minus_one_result) stores 1LL.
  if (!(smulll_overflow_minus_one_times_minus_one_result == 1LL))
    goto ERROR;

  // __builtin_smulll_overflow(-1LL, -1LL, &smulll_overflow_minus_one_times_minus_one_result) returns 0.
  if (!(smulll_overflow_minus_one_times_minus_one_overflow == 0))
    goto ERROR;


  // Tests for __builtin_umul_overflow.

  unsigned int umul_overflow_zero_times_unsigned_int_max_result;
  int umul_overflow_zero_times_unsigned_int_max_overflow;
  umul_overflow_zero_times_unsigned_int_max_overflow = __builtin_umul_overflow(0U, unsigned_int_max, &umul_overflow_zero_times_unsigned_int_max_result);

  // __builtin_umul_overflow(0U, 4294967295U, &umul_overflow_zero_times_unsigned_int_max_result) stores 0U.
  if (!(umul_overflow_zero_times_unsigned_int_max_result == 0U))
    goto ERROR;

  // __builtin_umul_overflow(0U, 4294967295U, &umul_overflow_zero_times_unsigned_int_max_result) returns 0.
  if (!(umul_overflow_zero_times_unsigned_int_max_overflow == 0))
    goto ERROR;


  unsigned int umul_overflow_one_times_unsigned_int_max_result;
  int umul_overflow_one_times_unsigned_int_max_overflow;
  umul_overflow_one_times_unsigned_int_max_overflow = __builtin_umul_overflow(1U, unsigned_int_max, &umul_overflow_one_times_unsigned_int_max_result);

  // __builtin_umul_overflow(1U, 4294967295U, &umul_overflow_one_times_unsigned_int_max_result) stores 4294967295U.
  if (!(umul_overflow_one_times_unsigned_int_max_result == unsigned_int_max))
    goto ERROR;

  // __builtin_umul_overflow(1U, 4294967295U, &umul_overflow_one_times_unsigned_int_max_result) returns 0.
  if (!(umul_overflow_one_times_unsigned_int_max_overflow == 0))
    goto ERROR;


  unsigned int umul_overflow_two_times_2147483647u_result;
  int umul_overflow_two_times_2147483647u_overflow;
  umul_overflow_two_times_2147483647u_overflow = __builtin_umul_overflow(2U, 2147483647U, &umul_overflow_two_times_2147483647u_result);

  // __builtin_umul_overflow(2U, 4294967295U / 2U, &umul_overflow_two_times_2147483647u_result) stores 4294967295U - 1U.
  if (!(umul_overflow_two_times_2147483647u_result == 4294967294U))
    goto ERROR;

  // __builtin_umul_overflow(2U, 4294967295U / 2U, &umul_overflow_two_times_2147483647u_result) returns 0.
  if (!(umul_overflow_two_times_2147483647u_overflow == 0))
    goto ERROR;


  unsigned int umul_overflow_two_times_2147483648u_result;
  int umul_overflow_two_times_2147483648u_overflow;
  umul_overflow_two_times_2147483648u_overflow = __builtin_umul_overflow(2U, 2147483648U, &umul_overflow_two_times_2147483648u_result);

  // __builtin_umul_overflow(2U, 4294967295U / 2U + 1U, &umul_overflow_two_times_2147483648u_result) stores 0U.
  if (!(umul_overflow_two_times_2147483648u_result == 0U))
    goto ERROR;

  // __builtin_umul_overflow(2U, 4294967295U / 2U + 1U, &umul_overflow_two_times_2147483648u_result) returns 1.
  if (!(umul_overflow_two_times_2147483648u_overflow == 1))
    goto ERROR;


  unsigned int umul_overflow_unsigned_int_max_times_unsigned_int_max_result;
  int umul_overflow_unsigned_int_max_times_unsigned_int_max_overflow;
  umul_overflow_unsigned_int_max_times_unsigned_int_max_overflow = __builtin_umul_overflow(unsigned_int_max, unsigned_int_max, &umul_overflow_unsigned_int_max_times_unsigned_int_max_result);

  // __builtin_umul_overflow(4294967295U, 4294967295U, &umul_overflow_unsigned_int_max_times_unsigned_int_max_result) stores 1U.
  if (!(umul_overflow_unsigned_int_max_times_unsigned_int_max_result == 1U))
    goto ERROR;

  // __builtin_umul_overflow(4294967295U, 4294967295U, &umul_overflow_unsigned_int_max_times_unsigned_int_max_result) returns 1.
  if (!(umul_overflow_unsigned_int_max_times_unsigned_int_max_overflow == 1))
    goto ERROR;


  unsigned int umul_overflow_unsigned_int_max_times_two_result;
  int umul_overflow_unsigned_int_max_times_two_overflow;
  umul_overflow_unsigned_int_max_times_two_overflow = __builtin_umul_overflow(unsigned_int_max, 2U, &umul_overflow_unsigned_int_max_times_two_result);

  // __builtin_umul_overflow(4294967295U, 2U, &umul_overflow_unsigned_int_max_times_two_result) stores 4294967295U - 1U.
  if (!(umul_overflow_unsigned_int_max_times_two_result == 4294967294U))
    goto ERROR;

  // __builtin_umul_overflow(4294967295U, 2U, &umul_overflow_unsigned_int_max_times_two_result) returns 1.
  if (!(umul_overflow_unsigned_int_max_times_two_overflow == 1))
    goto ERROR;


  // Tests for __builtin_umull_overflow.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)umull_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)umull_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  unsigned long int umull_overflow_zero_times_unsigned_long_max_result;
  int umull_overflow_zero_times_unsigned_long_max_overflow;
  umull_overflow_zero_times_unsigned_long_max_overflow = __builtin_umull_overflow(0UL, umull_overflow_unsigned_long_max, &umull_overflow_zero_times_unsigned_long_max_result);

  // ILP32: __builtin_umull_overflow(0UL, 4294967295UL, &umull_overflow_zero_times_unsigned_long_max_result) stores 0UL.
  // LP64: __builtin_umull_overflow(0UL, 18446744073709551615UL, &umull_overflow_zero_times_unsigned_long_max_result) stores 0UL.
  if (!(umull_overflow_zero_times_unsigned_long_max_result == 0UL))
    goto ERROR;

  // ILP32: __builtin_umull_overflow(0UL, 4294967295UL, &umull_overflow_zero_times_unsigned_long_max_result) returns 0.
  // LP64: __builtin_umull_overflow(0UL, 18446744073709551615UL, &umull_overflow_zero_times_unsigned_long_max_result) returns 0.
  if (!(umull_overflow_zero_times_unsigned_long_max_overflow == 0))
    goto ERROR;


  unsigned long int umull_overflow_one_times_unsigned_long_max_result;
  int umull_overflow_one_times_unsigned_long_max_overflow;
  umull_overflow_one_times_unsigned_long_max_overflow = __builtin_umull_overflow(1UL, umull_overflow_unsigned_long_max, &umull_overflow_one_times_unsigned_long_max_result);

  // ILP32: __builtin_umull_overflow(1UL, 4294967295UL, &umull_overflow_one_times_unsigned_long_max_result) stores 4294967295UL.
  // LP64: __builtin_umull_overflow(1UL, 18446744073709551615UL, &umull_overflow_one_times_unsigned_long_max_result) stores
  // 18446744073709551615UL.
  if (!(umull_overflow_one_times_unsigned_long_max_result == umull_overflow_unsigned_long_max))
    goto ERROR;

  // ILP32: __builtin_umull_overflow(1UL, 4294967295UL, &umull_overflow_one_times_unsigned_long_max_result) returns 0.
  // LP64: __builtin_umull_overflow(1UL, 18446744073709551615UL, &umull_overflow_one_times_unsigned_long_max_result) returns 0.
  if (!(umull_overflow_one_times_unsigned_long_max_overflow == 0))
    goto ERROR;


  unsigned long int umull_overflow_two_times_not_0ul_div_2ul_result;
  int umull_overflow_two_times_not_0ul_div_2ul_overflow;
  umull_overflow_two_times_not_0ul_div_2ul_overflow = __builtin_umull_overflow(2UL, (~0UL) / 2UL, &umull_overflow_two_times_not_0ul_div_2ul_result);

  // ILP32: __builtin_umull_overflow(2UL, 4294967295UL / 2UL, &umull_overflow_two_times_not_0ul_div_2ul_result) stores 4294967294UL.
  // LP64: __builtin_umull_overflow(2UL, 18446744073709551615UL / 2UL, &umull_overflow_two_times_not_0ul_div_2ul_result) stores
  // 18446744073709551614UL.
  if (!(umull_overflow_two_times_not_0ul_div_2ul_result == (~0UL) - 1UL))
    goto ERROR;

  // ILP32: __builtin_umull_overflow(2UL, 4294967295UL / 2UL, &umull_overflow_two_times_not_0ul_div_2ul_result) returns 0.
  // LP64: __builtin_umull_overflow(2UL, 18446744073709551615UL / 2UL, &umull_overflow_two_times_not_0ul_div_2ul_result) returns 0.
  if (!(umull_overflow_two_times_not_0ul_div_2ul_overflow == 0))
    goto ERROR;


  unsigned long int umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_result;
  int umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_overflow;
  umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_overflow = __builtin_umull_overflow(2UL, (~0UL) / 2UL + 1UL, &umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_result);

  // ILP32: __builtin_umull_overflow(2UL, 4294967295UL / 2UL + 1UL, &umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_result) stores 0UL.
  // LP64: __builtin_umull_overflow(2UL, 18446744073709551615UL / 2UL + 1UL, &umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_result)
  // stores 0UL.
  if (!(umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_result == 0UL))
    goto ERROR;

  // ILP32: __builtin_umull_overflow(2UL, 4294967295UL / 2UL + 1UL, &umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_result) returns 1.
  // LP64: __builtin_umull_overflow(2UL, 18446744073709551615UL / 2UL + 1UL, &umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_result)
  // returns 1.
  if (!(umull_overflow_two_times_not_0ul_div_2ul_plus_1ul_overflow == 1))
    goto ERROR;


  unsigned long int umull_overflow_unsigned_long_max_times_unsigned_long_max_result;
  int umull_overflow_unsigned_long_max_times_unsigned_long_max_overflow;
  umull_overflow_unsigned_long_max_times_unsigned_long_max_overflow = __builtin_umull_overflow(umull_overflow_unsigned_long_max, umull_overflow_unsigned_long_max, &umull_overflow_unsigned_long_max_times_unsigned_long_max_result);

  // ILP32: __builtin_umull_overflow(4294967295UL, 4294967295UL, &umull_overflow_unsigned_long_max_times_unsigned_long_max_result) stores
  // 1UL.
  // LP64: __builtin_umull_overflow(18446744073709551615UL, 18446744073709551615UL,
  // &umull_overflow_unsigned_long_max_times_unsigned_long_max_result) stores 1UL.
  if (!(umull_overflow_unsigned_long_max_times_unsigned_long_max_result == 1UL))
    goto ERROR;

  // ILP32: __builtin_umull_overflow(4294967295UL, 4294967295UL, &umull_overflow_unsigned_long_max_times_unsigned_long_max_result) returns
  // 1.
  // LP64: __builtin_umull_overflow(18446744073709551615UL, 18446744073709551615UL,
  // &umull_overflow_unsigned_long_max_times_unsigned_long_max_result) returns 1.
  if (!(umull_overflow_unsigned_long_max_times_unsigned_long_max_overflow == 1))
    goto ERROR;


  unsigned long int umull_overflow_unsigned_long_max_times_two_result;
  int umull_overflow_unsigned_long_max_times_two_overflow;
  umull_overflow_unsigned_long_max_times_two_overflow = __builtin_umull_overflow(umull_overflow_unsigned_long_max, 2UL, &umull_overflow_unsigned_long_max_times_two_result);

  // ILP32: __builtin_umull_overflow(4294967295UL, 2UL, &umull_overflow_unsigned_long_max_times_two_result) stores 4294967294UL.
  // LP64: __builtin_umull_overflow(18446744073709551615UL, 2UL, &umull_overflow_unsigned_long_max_times_two_result) stores
  // 18446744073709551614UL.
  if (!(umull_overflow_unsigned_long_max_times_two_result == (~0UL) - 1UL))
    goto ERROR;

  // ILP32: __builtin_umull_overflow(4294967295UL, 2UL, &umull_overflow_unsigned_long_max_times_two_result) returns 1.
  // LP64: __builtin_umull_overflow(18446744073709551615UL, 2UL, &umull_overflow_unsigned_long_max_times_two_result) returns 1.
  if (!(umull_overflow_unsigned_long_max_times_two_overflow == 1))
    goto ERROR;


  // Tests for __builtin_umulll_overflow.

  unsigned long long int umulll_overflow_zero_times_unsigned_long_long_max_result;
  int umulll_overflow_zero_times_unsigned_long_long_max_overflow;
  umulll_overflow_zero_times_unsigned_long_long_max_overflow = __builtin_umulll_overflow(0ULL, unsigned_long_long_max, &umulll_overflow_zero_times_unsigned_long_long_max_result);

  // __builtin_umulll_overflow(0ULL, 18446744073709551615ULL, &umulll_overflow_zero_times_unsigned_long_long_max_result) stores 0ULL.
  if (!(umulll_overflow_zero_times_unsigned_long_long_max_result == 0ULL))
    goto ERROR;

  // __builtin_umulll_overflow(0ULL, 18446744073709551615ULL, &umulll_overflow_zero_times_unsigned_long_long_max_result) returns 0.
  if (!(umulll_overflow_zero_times_unsigned_long_long_max_overflow == 0))
    goto ERROR;


  unsigned long long int umulll_overflow_one_times_unsigned_long_long_max_result;
  int umulll_overflow_one_times_unsigned_long_long_max_overflow;
  umulll_overflow_one_times_unsigned_long_long_max_overflow = __builtin_umulll_overflow(1ULL, unsigned_long_long_max, &umulll_overflow_one_times_unsigned_long_long_max_result);

  // __builtin_umulll_overflow(1ULL, 18446744073709551615ULL, &umulll_overflow_one_times_unsigned_long_long_max_result) stores
  // 18446744073709551615ULL.
  if (!(umulll_overflow_one_times_unsigned_long_long_max_result == unsigned_long_long_max))
    goto ERROR;

  // __builtin_umulll_overflow(1ULL, 18446744073709551615ULL, &umulll_overflow_one_times_unsigned_long_long_max_result) returns 0.
  if (!(umulll_overflow_one_times_unsigned_long_long_max_overflow == 0))
    goto ERROR;


  unsigned long long int umulll_overflow_two_times_9223372036854775807ull_result;
  int umulll_overflow_two_times_9223372036854775807ull_overflow;
  umulll_overflow_two_times_9223372036854775807ull_overflow = __builtin_umulll_overflow(2ULL, 9223372036854775807ULL, &umulll_overflow_two_times_9223372036854775807ull_result);

  // __builtin_umulll_overflow(2ULL, 18446744073709551615ULL / 2ULL, &umulll_overflow_two_times_9223372036854775807ull_result) stores
  // 18446744073709551614ULL.
  if (!(umulll_overflow_two_times_9223372036854775807ull_result == 18446744073709551614ULL))
    goto ERROR;

  // __builtin_umulll_overflow(2ULL, 18446744073709551615ULL / 2ULL, &umulll_overflow_two_times_9223372036854775807ull_result) returns 0.
  if (!(umulll_overflow_two_times_9223372036854775807ull_overflow == 0))
    goto ERROR;


  unsigned long long int umulll_overflow_two_times_9223372036854775808ull_result;
  int umulll_overflow_two_times_9223372036854775808ull_overflow;
  umulll_overflow_two_times_9223372036854775808ull_overflow = __builtin_umulll_overflow(2ULL, 9223372036854775808ULL, &umulll_overflow_two_times_9223372036854775808ull_result);

  // __builtin_umulll_overflow(2ULL, 18446744073709551615ULL / 2ULL + 1ULL, &umulll_overflow_two_times_9223372036854775808ull_result) stores
  // 0ULL.
  if (!(umulll_overflow_two_times_9223372036854775808ull_result == 0ULL))
    goto ERROR;

  // __builtin_umulll_overflow(2ULL, 18446744073709551615ULL / 2ULL + 1ULL, &umulll_overflow_two_times_9223372036854775808ull_result)
  // returns 1.
  if (!(umulll_overflow_two_times_9223372036854775808ull_overflow == 1))
    goto ERROR;


  unsigned long long int umulll_overflow_unsigned_long_long_max_times_unsigned_long_long_max_result;
  int umulll_overflow_unsigned_long_long_max_times_unsigned_long_long_max_overflow;
  umulll_overflow_unsigned_long_long_max_times_unsigned_long_long_max_overflow = __builtin_umulll_overflow(unsigned_long_long_max, unsigned_long_long_max, &umulll_overflow_unsigned_long_long_max_times_unsigned_long_long_max_result);

  // __builtin_umulll_overflow(18446744073709551615ULL, 18446744073709551615ULL,
  // &umulll_overflow_unsigned_long_long_max_times_unsigned_long_long_max_result) stores 1ULL.
  if (!(umulll_overflow_unsigned_long_long_max_times_unsigned_long_long_max_result == 1ULL))
    goto ERROR;

  // __builtin_umulll_overflow(18446744073709551615ULL, 18446744073709551615ULL,
  // &umulll_overflow_unsigned_long_long_max_times_unsigned_long_long_max_result) returns 1.
  if (!(umulll_overflow_unsigned_long_long_max_times_unsigned_long_long_max_overflow == 1))
    goto ERROR;


  unsigned long long int umulll_overflow_unsigned_long_long_max_times_two_result;
  int umulll_overflow_unsigned_long_long_max_times_two_overflow;
  umulll_overflow_unsigned_long_long_max_times_two_overflow = __builtin_umulll_overflow(unsigned_long_long_max, 2ULL, &umulll_overflow_unsigned_long_long_max_times_two_result);

  // __builtin_umulll_overflow(18446744073709551615ULL, 2ULL, &umulll_overflow_unsigned_long_long_max_times_two_result) stores
  // 18446744073709551614ULL.
  if (!(umulll_overflow_unsigned_long_long_max_times_two_result == 18446744073709551614ULL))
    goto ERROR;

  // __builtin_umulll_overflow(18446744073709551615ULL, 2ULL, &umulll_overflow_unsigned_long_long_max_times_two_result) returns 1.
  if (!(umulll_overflow_unsigned_long_long_max_times_two_overflow == 1))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
