// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>


void __VERIFIER_assert(int condition);

void __VERIFIER_assert(int condition) {
  if (!condition)
    goto ERROR;

  return;

ERROR:
  assert(0);
}


int main(void) {

  // Fixed typed test values are initialized directly and never modified.
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


  // Tests for __builtin_mul_overflow.

  const unsigned long int mul_overflow_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  __VERIFIER_assert((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)mul_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)mul_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max));


  const long int mul_overflow_long_max = (long int)((~0UL) >> 1);

  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  __VERIFIER_assert((sizeof(long int) == sizeof(int) && (int)mul_overflow_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)mul_overflow_long_max == (long long int)(long int)long_long_max));


  signed char mul_overflow_result_1;
  int mul_overflow_overflow_1 = __builtin_mul_overflow(signed_char_max, 1, &mul_overflow_result_1);

  // __builtin_mul_overflow(127, 1, &mul_overflow_result_1) stores 127.
  __VERIFIER_assert(mul_overflow_result_1 == signed_char_max);

  // __builtin_mul_overflow(127, 1, &mul_overflow_result_1) returns 0.
  __VERIFIER_assert(mul_overflow_overflow_1 == 0);


  signed char mul_overflow_result_2;
  int mul_overflow_overflow_2 = __builtin_mul_overflow(signed_char_min, -1, &mul_overflow_result_2);

  // __builtin_mul_overflow(-128, -1, &mul_overflow_result_2) stores -128.
  __VERIFIER_assert(mul_overflow_result_2 == signed_char_min);

  // __builtin_mul_overflow(-128, -1, &mul_overflow_result_2) returns 1.
  __VERIFIER_assert(mul_overflow_overflow_2 == 1);


  unsigned char mul_overflow_result_3;
  int mul_overflow_overflow_3 = __builtin_mul_overflow(unsigned_char_max, unsigned_char_max, &mul_overflow_result_3);

  // __builtin_mul_overflow(255U, 255U, &mul_overflow_result_3) stores 1U.
  __VERIFIER_assert(mul_overflow_result_3 == 1U);

  // __builtin_mul_overflow(255U, 255U, &mul_overflow_result_3) returns 1.
  __VERIFIER_assert(mul_overflow_overflow_3 == 1);


  short int mul_overflow_result_4;
  int mul_overflow_overflow_4 = __builtin_mul_overflow(short_max, 2, &mul_overflow_result_4);

  // __builtin_mul_overflow(32767, 2, &mul_overflow_result_4) stores -2.
  __VERIFIER_assert(mul_overflow_result_4 == -2);

  // __builtin_mul_overflow(32767, 2, &mul_overflow_result_4) returns 1.
  __VERIFIER_assert(mul_overflow_overflow_4 == 1);


  unsigned short int mul_overflow_result_5;
  int mul_overflow_overflow_5 = __builtin_mul_overflow(2U, 32767U, &mul_overflow_result_5);

  // __builtin_mul_overflow(2U, 65535U / 2U, &mul_overflow_result_5) stores 65535U - 1U.
  __VERIFIER_assert(mul_overflow_result_5 == 65534U);

  // __builtin_mul_overflow(2U, 65535U / 2U, &mul_overflow_result_5) returns 0.
  __VERIFIER_assert(mul_overflow_overflow_5 == 0);


  int mul_overflow_result_6;
  int mul_overflow_overflow_6 = __builtin_mul_overflow(int_min, -1, &mul_overflow_result_6);

  // __builtin_mul_overflow((-2147483647 - 1), -1, &mul_overflow_result_6) stores (-2147483647 - 1).
  __VERIFIER_assert(mul_overflow_result_6 == int_min);

  // __builtin_mul_overflow((-2147483647 - 1), -1, &mul_overflow_result_6) returns 1.
  __VERIFIER_assert(mul_overflow_overflow_6 == 1);


  unsigned int mul_overflow_result_7;
  int mul_overflow_overflow_7 = __builtin_mul_overflow(unsigned_int_max, unsigned_int_max, &mul_overflow_result_7);

  // __builtin_mul_overflow(4294967295U, 4294967295U, &mul_overflow_result_7) stores 1U.
  __VERIFIER_assert(mul_overflow_result_7 == 1U);

  // __builtin_mul_overflow(4294967295U, 4294967295U, &mul_overflow_result_7) returns 1.
  __VERIFIER_assert(mul_overflow_overflow_7 == 1);


  long int mul_overflow_result_8;
  int mul_overflow_overflow_8 = __builtin_mul_overflow(mul_overflow_long_max, 0L, &mul_overflow_result_8);

  // ILP32: __builtin_mul_overflow(2147483647L, 0L, &mul_overflow_result_8) stores 0L.
  // LP64: __builtin_mul_overflow(9223372036854775807L, 0L, &mul_overflow_result_8) stores 0L.
  __VERIFIER_assert(mul_overflow_result_8 == 0L);

  // ILP32: __builtin_mul_overflow(2147483647L, 0L, &mul_overflow_result_8) returns 0.
  // LP64: __builtin_mul_overflow(9223372036854775807L, 0L, &mul_overflow_result_8) returns 0.
  __VERIFIER_assert(mul_overflow_overflow_8 == 0);


  unsigned long int mul_overflow_result_9;
  int mul_overflow_overflow_9 = __builtin_mul_overflow(mul_overflow_unsigned_long_max, 2UL, &mul_overflow_result_9);

  // ILP32: __builtin_mul_overflow(4294967295UL, 2UL, &mul_overflow_result_9) stores 4294967294UL.
  // LP64: __builtin_mul_overflow(18446744073709551615UL, 2UL, &mul_overflow_result_9) stores 18446744073709551614UL.
  __VERIFIER_assert(mul_overflow_result_9 == (~0UL) - 1UL);

  // ILP32: __builtin_mul_overflow(4294967295UL, 2UL, &mul_overflow_result_9) returns 1.
  // LP64: __builtin_mul_overflow(18446744073709551615UL, 2UL, &mul_overflow_result_9) returns 1.
  __VERIFIER_assert(mul_overflow_overflow_9 == 1);


  long long int mul_overflow_result_10;
  int mul_overflow_overflow_10 = __builtin_mul_overflow(long_long_min, 0LL, &mul_overflow_result_10);

  // __builtin_mul_overflow(-9223372036854775808LL, 0LL, &mul_overflow_result_10) stores 0LL.
  __VERIFIER_assert(mul_overflow_result_10 == 0LL);

  // __builtin_mul_overflow(-9223372036854775808LL, 0LL, &mul_overflow_result_10) returns 0.
  __VERIFIER_assert(mul_overflow_overflow_10 == 0);


  unsigned long long int mul_overflow_result_11;
  int mul_overflow_overflow_11 = __builtin_mul_overflow(unsigned_long_long_max, unsigned_long_long_max, &mul_overflow_result_11);

  // __builtin_mul_overflow(18446744073709551615ULL, 18446744073709551615ULL, &mul_overflow_result_11) stores 1ULL.
  __VERIFIER_assert(mul_overflow_result_11 == 1ULL);

  // __builtin_mul_overflow(18446744073709551615ULL, 18446744073709551615ULL, &mul_overflow_result_11) returns 1.
  __VERIFIER_assert(mul_overflow_overflow_11 == 1);


  char mul_overflow_result_12;
  int mul_overflow_overflow_12 = __builtin_mul_overflow(char_max, 2, &mul_overflow_result_12);

  // __builtin_mul_overflow((char)127, 2, &mul_overflow_result_12) stores (char)-2.
  __VERIFIER_assert(mul_overflow_result_12 == char_minus_two);

  // __builtin_mul_overflow(127, 2, &mul_overflow_result_12) or __builtin_mul_overflow(255, 2, &mul_overflow_result_12)
  // returns 1, according to plain-char signedness.
  __VERIFIER_assert(mul_overflow_overflow_12 == 1);


  int mul_overflow_result_13;
  int mul_overflow_overflow_13 = __builtin_mul_overflow(-1LL, 1ULL, &mul_overflow_result_13);

  // __builtin_mul_overflow(-1LL, 1ULL, &mul_overflow_result_13) stores -1.
  __VERIFIER_assert(mul_overflow_result_13 == -1);

  // __builtin_mul_overflow(-1LL, 1ULL, &mul_overflow_result_13) returns 0.
  __VERIFIER_assert(mul_overflow_overflow_13 == 0);


  unsigned int mul_overflow_result_14;
  int mul_overflow_overflow_14 = __builtin_mul_overflow(-1LL, 2ULL, &mul_overflow_result_14);

  // __builtin_mul_overflow(-1LL, 2ULL, &mul_overflow_result_14) stores 4294967295U - 1U.
  __VERIFIER_assert(mul_overflow_result_14 == 4294967294U);

  // __builtin_mul_overflow(-1LL, 2ULL, &mul_overflow_result_14) returns 1.
  __VERIFIER_assert(mul_overflow_overflow_14 == 1);


  // Tests for __builtin_smul_overflow.

  int smul_overflow_result_1;
  int smul_overflow_overflow_1 = __builtin_smul_overflow(0, 0, &smul_overflow_result_1);

  // __builtin_smul_overflow(0, 0, &smul_overflow_result_1) stores 0.
  __VERIFIER_assert(smul_overflow_result_1 == 0);

  // __builtin_smul_overflow(0, 0, &smul_overflow_result_1) returns 0.
  __VERIFIER_assert(smul_overflow_overflow_1 == 0);


  int smul_overflow_result_2;
  int smul_overflow_overflow_2 = __builtin_smul_overflow(int_max, 0, &smul_overflow_result_2);

  // __builtin_smul_overflow(2147483647, 0, &smul_overflow_result_2) stores 0.
  __VERIFIER_assert(smul_overflow_result_2 == 0);

  // __builtin_smul_overflow(2147483647, 0, &smul_overflow_result_2) returns 0.
  __VERIFIER_assert(smul_overflow_overflow_2 == 0);


  int smul_overflow_result_3;
  int smul_overflow_overflow_3 = __builtin_smul_overflow(int_min, 0, &smul_overflow_result_3);

  // __builtin_smul_overflow((-2147483647 - 1), 0, &smul_overflow_result_3) stores 0.
  __VERIFIER_assert(smul_overflow_result_3 == 0);

  // __builtin_smul_overflow((-2147483647 - 1), 0, &smul_overflow_result_3) returns 0.
  __VERIFIER_assert(smul_overflow_overflow_3 == 0);


  int smul_overflow_result_4;
  int smul_overflow_overflow_4 = __builtin_smul_overflow(int_max, 1, &smul_overflow_result_4);

  // __builtin_smul_overflow(2147483647, 1, &smul_overflow_result_4) stores 2147483647.
  __VERIFIER_assert(smul_overflow_result_4 == int_max);

  // __builtin_smul_overflow(2147483647, 1, &smul_overflow_result_4) returns 0.
  __VERIFIER_assert(smul_overflow_overflow_4 == 0);


  int smul_overflow_result_5;
  int smul_overflow_overflow_5 = __builtin_smul_overflow(int_min, 1, &smul_overflow_result_5);

  // __builtin_smul_overflow((-2147483647 - 1), 1, &smul_overflow_result_5) stores (-2147483647 - 1).
  __VERIFIER_assert(smul_overflow_result_5 == int_min);

  // __builtin_smul_overflow((-2147483647 - 1), 1, &smul_overflow_result_5) returns 0.
  __VERIFIER_assert(smul_overflow_overflow_5 == 0);


  int smul_overflow_result_6;
  int smul_overflow_overflow_6 = __builtin_smul_overflow(int_max, -1, &smul_overflow_result_6);

  // __builtin_smul_overflow(2147483647, -1, &smul_overflow_result_6) stores -2147483647.
  __VERIFIER_assert(smul_overflow_result_6 == -int_max);

  // __builtin_smul_overflow(2147483647, -1, &smul_overflow_result_6) returns 0.
  __VERIFIER_assert(smul_overflow_overflow_6 == 0);


  int smul_overflow_result_7;
  int smul_overflow_overflow_7 = __builtin_smul_overflow(int_min, -1, &smul_overflow_result_7);

  // __builtin_smul_overflow((-2147483647 - 1), -1, &smul_overflow_result_7) stores (-2147483647 - 1).
  __VERIFIER_assert(smul_overflow_result_7 == int_min);

  // __builtin_smul_overflow((-2147483647 - 1), -1, &smul_overflow_result_7) returns 1.
  __VERIFIER_assert(smul_overflow_overflow_7 == 1);


  int smul_overflow_result_8;
  int smul_overflow_overflow_8 = __builtin_smul_overflow(int_max, 2, &smul_overflow_result_8);

  // __builtin_smul_overflow(2147483647, 2, &smul_overflow_result_8) stores -2.
  __VERIFIER_assert(smul_overflow_result_8 == -2);

  // __builtin_smul_overflow(2147483647, 2, &smul_overflow_result_8) returns 1.
  __VERIFIER_assert(smul_overflow_overflow_8 == 1);


  int smul_overflow_result_9;
  int smul_overflow_overflow_9 = __builtin_smul_overflow(int_min, 2, &smul_overflow_result_9);

  // __builtin_smul_overflow((-2147483647 - 1), 2, &smul_overflow_result_9) stores 0.
  __VERIFIER_assert(smul_overflow_result_9 == 0);

  // __builtin_smul_overflow((-2147483647 - 1), 2, &smul_overflow_result_9) returns 1.
  __VERIFIER_assert(smul_overflow_overflow_9 == 1);


  int smul_overflow_result_10;
  int smul_overflow_overflow_10 = __builtin_smul_overflow(-1, -1, &smul_overflow_result_10);

  // __builtin_smul_overflow(-1, -1, &smul_overflow_result_10) stores 1.
  __VERIFIER_assert(smul_overflow_result_10 == 1);

  // __builtin_smul_overflow(-1, -1, &smul_overflow_result_10) returns 0.
  __VERIFIER_assert(smul_overflow_overflow_10 == 0);


  // Tests for __builtin_smull_overflow.

  const unsigned long int smull_overflow_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  __VERIFIER_assert((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)smull_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)smull_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max));


  const long int smull_overflow_long_max = (long int)((~0UL) >> 1);

  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  __VERIFIER_assert((sizeof(long int) == sizeof(int) && (int)smull_overflow_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)smull_overflow_long_max == (long long int)(long int)long_long_max));


  const long int smull_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);

  // ILP32: The calculated long minimum must equal int minimum -2147483648.
  // LP64: The calculated long minimum must equal long long minimum -9223372036854775808.
  __VERIFIER_assert((sizeof(long int) == sizeof(int) && (int)smull_overflow_long_min == int_min) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)smull_overflow_long_min ==
            (long long int)(long int)long_long_min));


  long int smull_overflow_result_1;
  int smull_overflow_overflow_1 = __builtin_smull_overflow(0L, 0L, &smull_overflow_result_1);

  // __builtin_smull_overflow(0L, 0L, &smull_overflow_result_1) stores 0L.
  __VERIFIER_assert(smull_overflow_result_1 == 0L);

  // __builtin_smull_overflow(0L, 0L, &smull_overflow_result_1) returns 0.
  __VERIFIER_assert(smull_overflow_overflow_1 == 0);


  long int smull_overflow_result_2;
  int smull_overflow_overflow_2 = __builtin_smull_overflow(smull_overflow_long_max, 0L, &smull_overflow_result_2);

  // ILP32: __builtin_smull_overflow(2147483647L, 0L, &smull_overflow_result_2) stores 0L.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 0L, &smull_overflow_result_2) stores 0L.
  __VERIFIER_assert(smull_overflow_result_2 == 0L);

  // ILP32: __builtin_smull_overflow(2147483647L, 0L, &smull_overflow_result_2) returns 0.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 0L, &smull_overflow_result_2) returns 0.
  __VERIFIER_assert(smull_overflow_overflow_2 == 0);


  long int smull_overflow_result_3;
  int smull_overflow_overflow_3 = __builtin_smull_overflow(smull_overflow_long_min, 0L, &smull_overflow_result_3);

  // ILP32: __builtin_smull_overflow(-2147483648L, 0L, &smull_overflow_result_3) stores 0L.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 0L, &smull_overflow_result_3) stores 0L.
  __VERIFIER_assert(smull_overflow_result_3 == 0L);

  // ILP32: __builtin_smull_overflow(-2147483648L, 0L, &smull_overflow_result_3) returns 0.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 0L, &smull_overflow_result_3) returns 0.
  __VERIFIER_assert(smull_overflow_overflow_3 == 0);


  long int smull_overflow_result_4;
  int smull_overflow_overflow_4 = __builtin_smull_overflow(smull_overflow_long_max, 1L, &smull_overflow_result_4);

  // ILP32: __builtin_smull_overflow(2147483647L, 1L, &smull_overflow_result_4) stores 2147483647L.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 1L, &smull_overflow_result_4) stores 9223372036854775807L.
  __VERIFIER_assert(smull_overflow_result_4 == smull_overflow_long_max);

  // ILP32: __builtin_smull_overflow(2147483647L, 1L, &smull_overflow_result_4) returns 0.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 1L, &smull_overflow_result_4) returns 0.
  __VERIFIER_assert(smull_overflow_overflow_4 == 0);


  long int smull_overflow_result_5;
  int smull_overflow_overflow_5 = __builtin_smull_overflow(smull_overflow_long_min, 1L, &smull_overflow_result_5);

  // ILP32: __builtin_smull_overflow(-2147483648L, 1L, &smull_overflow_result_5) stores -2147483648L.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 1L, &smull_overflow_result_5) stores -9223372036854775808L.
  __VERIFIER_assert(smull_overflow_result_5 == smull_overflow_long_min);

  // ILP32: __builtin_smull_overflow(-2147483648L, 1L, &smull_overflow_result_5) returns 0.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 1L, &smull_overflow_result_5) returns 0.
  __VERIFIER_assert(smull_overflow_overflow_5 == 0);


  long int smull_overflow_result_6;
  int smull_overflow_overflow_6 = __builtin_smull_overflow(smull_overflow_long_max, -1L, &smull_overflow_result_6);

  // ILP32: __builtin_smull_overflow(2147483647L, -1L, &smull_overflow_result_6) stores -2147483647L.
  // LP64: __builtin_smull_overflow(9223372036854775807L, -1L, &smull_overflow_result_6) stores -9223372036854775807L.
  __VERIFIER_assert(smull_overflow_result_6 == - ((long int)((~0UL) >> 1)));

  // ILP32: __builtin_smull_overflow(2147483647L, -1L, &smull_overflow_result_6) returns 0.
  // LP64: __builtin_smull_overflow(9223372036854775807L, -1L, &smull_overflow_result_6) returns 0.
  __VERIFIER_assert(smull_overflow_overflow_6 == 0);


  long int smull_overflow_result_7;
  int smull_overflow_overflow_7 = __builtin_smull_overflow(smull_overflow_long_min, -1L, &smull_overflow_result_7);

  // ILP32: __builtin_smull_overflow(-2147483648L, -1L, &smull_overflow_result_7) stores -2147483648L.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, -1L, &smull_overflow_result_7) stores -9223372036854775808L.
  __VERIFIER_assert(smull_overflow_result_7 == smull_overflow_long_min);

  // ILP32: __builtin_smull_overflow(-2147483648L, -1L, &smull_overflow_result_7) returns 1.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, -1L, &smull_overflow_result_7) returns 1.
  __VERIFIER_assert(smull_overflow_overflow_7 == 1);


  long int smull_overflow_result_8;
  int smull_overflow_overflow_8 = __builtin_smull_overflow(smull_overflow_long_max, 2L, &smull_overflow_result_8);

  // ILP32: __builtin_smull_overflow(2147483647L, 2L, &smull_overflow_result_8) stores -2L.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 2L, &smull_overflow_result_8) stores -2L.
  __VERIFIER_assert(smull_overflow_result_8 == -2L);

  // ILP32: __builtin_smull_overflow(2147483647L, 2L, &smull_overflow_result_8) returns 1.
  // LP64: __builtin_smull_overflow(9223372036854775807L, 2L, &smull_overflow_result_8) returns 1.
  __VERIFIER_assert(smull_overflow_overflow_8 == 1);


  long int smull_overflow_result_9;
  int smull_overflow_overflow_9 = __builtin_smull_overflow(smull_overflow_long_min, 2L, &smull_overflow_result_9);

  // ILP32: __builtin_smull_overflow(-2147483648L, 2L, &smull_overflow_result_9) stores 0L.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 2L, &smull_overflow_result_9) stores 0L.
  __VERIFIER_assert(smull_overflow_result_9 == 0L);

  // ILP32: __builtin_smull_overflow(-2147483648L, 2L, &smull_overflow_result_9) returns 1.
  // LP64: __builtin_smull_overflow(-9223372036854775808L, 2L, &smull_overflow_result_9) returns 1.
  __VERIFIER_assert(smull_overflow_overflow_9 == 1);


  long int smull_overflow_result_10;
  int smull_overflow_overflow_10 = __builtin_smull_overflow(-1L, -1L, &smull_overflow_result_10);

  // __builtin_smull_overflow(-1L, -1L, &smull_overflow_result_10) stores 1L.
  __VERIFIER_assert(smull_overflow_result_10 == 1L);

  // __builtin_smull_overflow(-1L, -1L, &smull_overflow_result_10) returns 0.
  __VERIFIER_assert(smull_overflow_overflow_10 == 0);


  // Tests for __builtin_smulll_overflow.

  long long int smulll_overflow_result_1;
  int smulll_overflow_overflow_1 = __builtin_smulll_overflow(0LL, 0LL, &smulll_overflow_result_1);

  // __builtin_smulll_overflow(0LL, 0LL, &smulll_overflow_result_1) stores 0LL.
  __VERIFIER_assert(smulll_overflow_result_1 == 0LL);

  // __builtin_smulll_overflow(0LL, 0LL, &smulll_overflow_result_1) returns 0.
  __VERIFIER_assert(smulll_overflow_overflow_1 == 0);


  long long int smulll_overflow_result_2;
  int smulll_overflow_overflow_2 = __builtin_smulll_overflow(long_long_max, 0LL, &smulll_overflow_result_2);

  // __builtin_smulll_overflow(9223372036854775807LL, 0LL, &smulll_overflow_result_2) stores 0LL.
  __VERIFIER_assert(smulll_overflow_result_2 == 0LL);

  // __builtin_smulll_overflow(9223372036854775807LL, 0LL, &smulll_overflow_result_2) returns 0.
  __VERIFIER_assert(smulll_overflow_overflow_2 == 0);


  long long int smulll_overflow_result_3;
  int smulll_overflow_overflow_3 = __builtin_smulll_overflow(long_long_min, 0LL, &smulll_overflow_result_3);

  // __builtin_smulll_overflow(-9223372036854775808LL, 0LL, &smulll_overflow_result_3) stores 0LL.
  __VERIFIER_assert(smulll_overflow_result_3 == 0LL);

  // __builtin_smulll_overflow(-9223372036854775808LL, 0LL, &smulll_overflow_result_3) returns 0.
  __VERIFIER_assert(smulll_overflow_overflow_3 == 0);


  long long int smulll_overflow_result_4;
  int smulll_overflow_overflow_4 = __builtin_smulll_overflow(long_long_max, 1LL, &smulll_overflow_result_4);

  // __builtin_smulll_overflow(9223372036854775807LL, 1LL, &smulll_overflow_result_4) stores 9223372036854775807LL.
  __VERIFIER_assert(smulll_overflow_result_4 == long_long_max);

  // __builtin_smulll_overflow(9223372036854775807LL, 1LL, &smulll_overflow_result_4) returns 0.
  __VERIFIER_assert(smulll_overflow_overflow_4 == 0);


  long long int smulll_overflow_result_5;
  int smulll_overflow_overflow_5 = __builtin_smulll_overflow(long_long_min, 1LL, &smulll_overflow_result_5);

  // __builtin_smulll_overflow(-9223372036854775808LL, 1LL, &smulll_overflow_result_5) stores -9223372036854775808LL.
  __VERIFIER_assert(smulll_overflow_result_5 == long_long_min);

  // __builtin_smulll_overflow(-9223372036854775808LL, 1LL, &smulll_overflow_result_5) returns 0.
  __VERIFIER_assert(smulll_overflow_overflow_5 == 0);


  long long int smulll_overflow_result_6;
  int smulll_overflow_overflow_6 = __builtin_smulll_overflow(long_long_max, -1LL, &smulll_overflow_result_6);

  // __builtin_smulll_overflow(9223372036854775807LL, -1LL, &smulll_overflow_result_6) stores -9223372036854775807LL.
  __VERIFIER_assert(smulll_overflow_result_6 == -long_long_max);

  // __builtin_smulll_overflow(9223372036854775807LL, -1LL, &smulll_overflow_result_6) returns 0.
  __VERIFIER_assert(smulll_overflow_overflow_6 == 0);


  long long int smulll_overflow_result_7;
  int smulll_overflow_overflow_7 = __builtin_smulll_overflow(long_long_min, -1LL, &smulll_overflow_result_7);

  // __builtin_smulll_overflow(-9223372036854775808LL, -1LL, &smulll_overflow_result_7) stores -9223372036854775808LL.
  __VERIFIER_assert(smulll_overflow_result_7 == long_long_min);

  // __builtin_smulll_overflow(-9223372036854775808LL, -1LL, &smulll_overflow_result_7) returns 1.
  __VERIFIER_assert(smulll_overflow_overflow_7 == 1);


  long long int smulll_overflow_result_8;
  int smulll_overflow_overflow_8 = __builtin_smulll_overflow(long_long_max, 2LL, &smulll_overflow_result_8);

  // __builtin_smulll_overflow(9223372036854775807LL, 2LL, &smulll_overflow_result_8) stores -2LL.
  __VERIFIER_assert(smulll_overflow_result_8 == -2LL);

  // __builtin_smulll_overflow(9223372036854775807LL, 2LL, &smulll_overflow_result_8) returns 1.
  __VERIFIER_assert(smulll_overflow_overflow_8 == 1);


  long long int smulll_overflow_result_9;
  int smulll_overflow_overflow_9 = __builtin_smulll_overflow(long_long_min, 2LL, &smulll_overflow_result_9);

  // __builtin_smulll_overflow(-9223372036854775808LL, 2LL, &smulll_overflow_result_9) stores 0LL.
  __VERIFIER_assert(smulll_overflow_result_9 == 0LL);

  // __builtin_smulll_overflow(-9223372036854775808LL, 2LL, &smulll_overflow_result_9) returns 1.
  __VERIFIER_assert(smulll_overflow_overflow_9 == 1);


  long long int smulll_overflow_result_10;
  int smulll_overflow_overflow_10 = __builtin_smulll_overflow(-1LL, -1LL, &smulll_overflow_result_10);

  // __builtin_smulll_overflow(-1LL, -1LL, &smulll_overflow_result_10) stores 1LL.
  __VERIFIER_assert(smulll_overflow_result_10 == 1LL);

  // __builtin_smulll_overflow(-1LL, -1LL, &smulll_overflow_result_10) returns 0.
  __VERIFIER_assert(smulll_overflow_overflow_10 == 0);


  // Tests for __builtin_umul_overflow.

  unsigned int umul_overflow_result_1;
  int umul_overflow_overflow_1 = __builtin_umul_overflow(0U, unsigned_int_max, &umul_overflow_result_1);

  // __builtin_umul_overflow(0U, 4294967295U, &umul_overflow_result_1) stores 0U.
  __VERIFIER_assert(umul_overflow_result_1 == 0U);

  // __builtin_umul_overflow(0U, 4294967295U, &umul_overflow_result_1) returns 0.
  __VERIFIER_assert(umul_overflow_overflow_1 == 0);


  unsigned int umul_overflow_result_2;
  int umul_overflow_overflow_2 = __builtin_umul_overflow(1U, unsigned_int_max, &umul_overflow_result_2);

  // __builtin_umul_overflow(1U, 4294967295U, &umul_overflow_result_2) stores 4294967295U.
  __VERIFIER_assert(umul_overflow_result_2 == unsigned_int_max);

  // __builtin_umul_overflow(1U, 4294967295U, &umul_overflow_result_2) returns 0.
  __VERIFIER_assert(umul_overflow_overflow_2 == 0);


  unsigned int umul_overflow_result_3;
  int umul_overflow_overflow_3 = __builtin_umul_overflow(2U, 2147483647U, &umul_overflow_result_3);

  // __builtin_umul_overflow(2U, 4294967295U / 2U, &umul_overflow_result_3) stores 4294967295U - 1U.
  __VERIFIER_assert(umul_overflow_result_3 == 4294967294U);

  // __builtin_umul_overflow(2U, 4294967295U / 2U, &umul_overflow_result_3) returns 0.
  __VERIFIER_assert(umul_overflow_overflow_3 == 0);


  unsigned int umul_overflow_result_4;
  int umul_overflow_overflow_4 = __builtin_umul_overflow(2U, 2147483648U, &umul_overflow_result_4);

  // __builtin_umul_overflow(2U, 4294967295U / 2U + 1U, &umul_overflow_result_4) stores 0U.
  __VERIFIER_assert(umul_overflow_result_4 == 0U);

  // __builtin_umul_overflow(2U, 4294967295U / 2U + 1U, &umul_overflow_result_4) returns 1.
  __VERIFIER_assert(umul_overflow_overflow_4 == 1);


  unsigned int umul_overflow_result_5;
  int umul_overflow_overflow_5 = __builtin_umul_overflow(unsigned_int_max, unsigned_int_max, &umul_overflow_result_5);

  // __builtin_umul_overflow(4294967295U, 4294967295U, &umul_overflow_result_5) stores 1U.
  __VERIFIER_assert(umul_overflow_result_5 == 1U);

  // __builtin_umul_overflow(4294967295U, 4294967295U, &umul_overflow_result_5) returns 1.
  __VERIFIER_assert(umul_overflow_overflow_5 == 1);


  unsigned int umul_overflow_result_6;
  int umul_overflow_overflow_6 = __builtin_umul_overflow(unsigned_int_max, 2U, &umul_overflow_result_6);

  // __builtin_umul_overflow(4294967295U, 2U, &umul_overflow_result_6) stores 4294967295U - 1U.
  __VERIFIER_assert(umul_overflow_result_6 == 4294967294U);

  // __builtin_umul_overflow(4294967295U, 2U, &umul_overflow_result_6) returns 1.
  __VERIFIER_assert(umul_overflow_overflow_6 == 1);


  // Tests for __builtin_umull_overflow.

  const unsigned long int umull_overflow_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  __VERIFIER_assert((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)umull_overflow_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)umull_overflow_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max));


  unsigned long int umull_overflow_result_1;
  int umull_overflow_overflow_1 = __builtin_umull_overflow(0UL, umull_overflow_unsigned_long_max, &umull_overflow_result_1);

  // ILP32: __builtin_umull_overflow(0UL, 4294967295UL, &umull_overflow_result_1) stores 0UL.
  // LP64: __builtin_umull_overflow(0UL, 18446744073709551615UL, &umull_overflow_result_1) stores 0UL.
  __VERIFIER_assert(umull_overflow_result_1 == 0UL);

  // ILP32: __builtin_umull_overflow(0UL, 4294967295UL, &umull_overflow_result_1) returns 0.
  // LP64: __builtin_umull_overflow(0UL, 18446744073709551615UL, &umull_overflow_result_1) returns 0.
  __VERIFIER_assert(umull_overflow_overflow_1 == 0);


  unsigned long int umull_overflow_result_2;
  int umull_overflow_overflow_2 = __builtin_umull_overflow(1UL, umull_overflow_unsigned_long_max, &umull_overflow_result_2);

  // ILP32: __builtin_umull_overflow(1UL, 4294967295UL, &umull_overflow_result_2) stores 4294967295UL.
  // LP64: __builtin_umull_overflow(1UL, 18446744073709551615UL, &umull_overflow_result_2) stores 18446744073709551615UL.
  __VERIFIER_assert(umull_overflow_result_2 == umull_overflow_unsigned_long_max);

  // ILP32: __builtin_umull_overflow(1UL, 4294967295UL, &umull_overflow_result_2) returns 0.
  // LP64: __builtin_umull_overflow(1UL, 18446744073709551615UL, &umull_overflow_result_2) returns 0.
  __VERIFIER_assert(umull_overflow_overflow_2 == 0);


  unsigned long int umull_overflow_result_3;
  int umull_overflow_overflow_3 = __builtin_umull_overflow(2UL, (~0UL) / 2UL, &umull_overflow_result_3);

  // ILP32: __builtin_umull_overflow(2UL, 4294967295UL / 2UL, &umull_overflow_result_3) stores 4294967294UL.
  // LP64: __builtin_umull_overflow(2UL, 18446744073709551615UL / 2UL, &umull_overflow_result_3) stores 18446744073709551614UL.
  __VERIFIER_assert(umull_overflow_result_3 == (~0UL) - 1UL);

  // ILP32: __builtin_umull_overflow(2UL, 4294967295UL / 2UL, &umull_overflow_result_3) returns 0.
  // LP64: __builtin_umull_overflow(2UL, 18446744073709551615UL / 2UL, &umull_overflow_result_3) returns 0.
  __VERIFIER_assert(umull_overflow_overflow_3 == 0);


  unsigned long int umull_overflow_result_4;
  int umull_overflow_overflow_4 = __builtin_umull_overflow(2UL, (~0UL) / 2UL + 1UL, &umull_overflow_result_4);

  // ILP32: __builtin_umull_overflow(2UL, 4294967295UL / 2UL + 1UL, &umull_overflow_result_4) stores 0UL.
  // LP64: __builtin_umull_overflow(2UL, 18446744073709551615UL / 2UL + 1UL, &umull_overflow_result_4) stores 0UL.
  __VERIFIER_assert(umull_overflow_result_4 == 0UL);

  // ILP32: __builtin_umull_overflow(2UL, 4294967295UL / 2UL + 1UL, &umull_overflow_result_4) returns 1.
  // LP64: __builtin_umull_overflow(2UL, 18446744073709551615UL / 2UL + 1UL, &umull_overflow_result_4) returns 1.
  __VERIFIER_assert(umull_overflow_overflow_4 == 1);


  unsigned long int umull_overflow_result_5;
  int umull_overflow_overflow_5 = __builtin_umull_overflow(umull_overflow_unsigned_long_max, umull_overflow_unsigned_long_max, &umull_overflow_result_5);

  // ILP32: __builtin_umull_overflow(4294967295UL, 4294967295UL, &umull_overflow_result_5) stores 1UL.
  // LP64: __builtin_umull_overflow(18446744073709551615UL, 18446744073709551615UL, &umull_overflow_result_5) stores 1UL.
  __VERIFIER_assert(umull_overflow_result_5 == 1UL);

  // ILP32: __builtin_umull_overflow(4294967295UL, 4294967295UL, &umull_overflow_result_5) returns 1.
  // LP64: __builtin_umull_overflow(18446744073709551615UL, 18446744073709551615UL, &umull_overflow_result_5) returns 1.
  __VERIFIER_assert(umull_overflow_overflow_5 == 1);


  unsigned long int umull_overflow_result_6;
  int umull_overflow_overflow_6 = __builtin_umull_overflow(umull_overflow_unsigned_long_max, 2UL, &umull_overflow_result_6);

  // ILP32: __builtin_umull_overflow(4294967295UL, 2UL, &umull_overflow_result_6) stores 4294967294UL.
  // LP64: __builtin_umull_overflow(18446744073709551615UL, 2UL, &umull_overflow_result_6) stores 18446744073709551614UL.
  __VERIFIER_assert(umull_overflow_result_6 == (~0UL) - 1UL);

  // ILP32: __builtin_umull_overflow(4294967295UL, 2UL, &umull_overflow_result_6) returns 1.
  // LP64: __builtin_umull_overflow(18446744073709551615UL, 2UL, &umull_overflow_result_6) returns 1.
  __VERIFIER_assert(umull_overflow_overflow_6 == 1);


  // Tests for __builtin_umulll_overflow.

  unsigned long long int umulll_overflow_result_1;
  int umulll_overflow_overflow_1 = __builtin_umulll_overflow(0ULL, unsigned_long_long_max, &umulll_overflow_result_1);

  // __builtin_umulll_overflow(0ULL, 18446744073709551615ULL, &umulll_overflow_result_1) stores 0ULL.
  __VERIFIER_assert(umulll_overflow_result_1 == 0ULL);

  // __builtin_umulll_overflow(0ULL, 18446744073709551615ULL, &umulll_overflow_result_1) returns 0.
  __VERIFIER_assert(umulll_overflow_overflow_1 == 0);


  unsigned long long int umulll_overflow_result_2;
  int umulll_overflow_overflow_2 = __builtin_umulll_overflow(1ULL, unsigned_long_long_max, &umulll_overflow_result_2);

  // __builtin_umulll_overflow(1ULL, 18446744073709551615ULL, &umulll_overflow_result_2) stores 18446744073709551615ULL.
  __VERIFIER_assert(umulll_overflow_result_2 == unsigned_long_long_max);

  // __builtin_umulll_overflow(1ULL, 18446744073709551615ULL, &umulll_overflow_result_2) returns 0.
  __VERIFIER_assert(umulll_overflow_overflow_2 == 0);


  unsigned long long int umulll_overflow_result_3;
  int umulll_overflow_overflow_3 = __builtin_umulll_overflow(2ULL, 9223372036854775807ULL, &umulll_overflow_result_3);

  // __builtin_umulll_overflow(2ULL, 18446744073709551615ULL / 2ULL, &umulll_overflow_result_3) stores 18446744073709551614ULL.
  __VERIFIER_assert(umulll_overflow_result_3 == 18446744073709551614ULL);

  // __builtin_umulll_overflow(2ULL, 18446744073709551615ULL / 2ULL, &umulll_overflow_result_3) returns 0.
  __VERIFIER_assert(umulll_overflow_overflow_3 == 0);


  unsigned long long int umulll_overflow_result_4;
  int umulll_overflow_overflow_4 = __builtin_umulll_overflow(2ULL, 9223372036854775808ULL, &umulll_overflow_result_4);

  // __builtin_umulll_overflow(2ULL, 18446744073709551615ULL / 2ULL + 1ULL, &umulll_overflow_result_4) stores 0ULL.
  __VERIFIER_assert(umulll_overflow_result_4 == 0ULL);

  // __builtin_umulll_overflow(2ULL, 18446744073709551615ULL / 2ULL + 1ULL, &umulll_overflow_result_4) returns 1.
  __VERIFIER_assert(umulll_overflow_overflow_4 == 1);


  unsigned long long int umulll_overflow_result_5;
  int umulll_overflow_overflow_5 = __builtin_umulll_overflow(unsigned_long_long_max, unsigned_long_long_max, &umulll_overflow_result_5);

  // __builtin_umulll_overflow(18446744073709551615ULL, 18446744073709551615ULL, &umulll_overflow_result_5) stores 1ULL.
  __VERIFIER_assert(umulll_overflow_result_5 == 1ULL);

  // __builtin_umulll_overflow(18446744073709551615ULL, 18446744073709551615ULL, &umulll_overflow_result_5) returns 1.
  __VERIFIER_assert(umulll_overflow_overflow_5 == 1);


  unsigned long long int umulll_overflow_result_6;
  int umulll_overflow_overflow_6 = __builtin_umulll_overflow(unsigned_long_long_max, 2ULL, &umulll_overflow_result_6);

  // __builtin_umulll_overflow(18446744073709551615ULL, 2ULL, &umulll_overflow_result_6) stores 18446744073709551614ULL.
  __VERIFIER_assert(umulll_overflow_result_6 == 18446744073709551614ULL);

  // __builtin_umulll_overflow(18446744073709551615ULL, 2ULL, &umulll_overflow_result_6) returns 1.
  __VERIFIER_assert(umulll_overflow_overflow_6 == 1);
  return 0;
}
