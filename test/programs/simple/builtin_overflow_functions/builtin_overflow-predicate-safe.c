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
  const signed char signed_char_zero = 0;
  const unsigned char unsigned_char_zero = 0U;
  const short int short_zero = 0;
  const unsigned short int unsigned_short_zero = 0U;
  const int int_zero = 0;
  const unsigned int unsigned_int_zero = 0U;
  const long int long_zero = 0L;
  const unsigned long int unsigned_long_zero = 0UL;
  const long long int long_long_zero = 0LL;
  const unsigned long long int unsigned_long_long_zero = 0ULL;
  const char char_zero = 0;


  // Tests for __builtin_add_overflow_p.

  const unsigned long int add_overflow_p_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  __VERIFIER_assert((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)add_overflow_p_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)add_overflow_p_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max));


  const long int add_overflow_p_long_max = (long int)((~0UL) >> 1);

  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  __VERIFIER_assert((sizeof(long int) == sizeof(int) && (int)add_overflow_p_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)add_overflow_p_long_max == (long long int)(long int)long_long_max));


  int add_overflow_p_overflow_1 = __builtin_add_overflow_p(signed_char_max, 0, signed_char_zero);

  // __builtin_add_overflow_p(127, 0, (signed char)0) returns 0.
  __VERIFIER_assert(add_overflow_p_overflow_1 == 0);


  int add_overflow_p_overflow_2 = __builtin_add_overflow_p(signed_char_max, 1, signed_char_zero);

  // __builtin_add_overflow_p(127, 1, (signed char)0) returns 1.
  __VERIFIER_assert(add_overflow_p_overflow_2 == 1);


  int add_overflow_p_overflow_3 = __builtin_add_overflow_p(unsigned_char_max, 1, unsigned_char_zero);

  // __builtin_add_overflow_p(255U, 1, (unsigned char)0) returns 1.
  __VERIFIER_assert(add_overflow_p_overflow_3 == 1);


  int add_overflow_p_overflow_4 = __builtin_add_overflow_p(short_min, -1, short_zero);

  // __builtin_add_overflow_p(-32768, -1, (short int)0) returns 1.
  __VERIFIER_assert(add_overflow_p_overflow_4 == 1);


  int add_overflow_p_overflow_5 = __builtin_add_overflow_p(-1, 1U, unsigned_short_zero);

  // __builtin_add_overflow_p(-1, 1U, (unsigned short int)0) returns 0.
  __VERIFIER_assert(add_overflow_p_overflow_5 == 0);


  int add_overflow_p_overflow_6 = __builtin_add_overflow_p(int_max, 1, int_zero);

  // __builtin_add_overflow_p(2147483647, 1, (int)0) returns 1.
  __VERIFIER_assert(add_overflow_p_overflow_6 == 1);


  int add_overflow_p_overflow_7 = __builtin_add_overflow_p(unsigned_int_max, 1U, unsigned_int_zero);

  // __builtin_add_overflow_p(4294967295U, 1U, (unsigned int)0) returns 1.
  __VERIFIER_assert(add_overflow_p_overflow_7 == 1);


  int add_overflow_p_overflow_8 = __builtin_add_overflow_p(add_overflow_p_long_max, 0L, long_zero);

  // ILP32: __builtin_add_overflow_p(2147483647L, 0L, (long int)0) returns 0.
  // LP64: __builtin_add_overflow_p(9223372036854775807L, 0L, (long int)0) returns 0.
  __VERIFIER_assert(add_overflow_p_overflow_8 == 0);


  int add_overflow_p_overflow_9 = __builtin_add_overflow_p(add_overflow_p_unsigned_long_max, 1UL, unsigned_long_zero);

  // ILP32: __builtin_add_overflow_p(4294967295UL, 1UL, (unsigned long int)0) returns 1.
  // LP64: __builtin_add_overflow_p(18446744073709551615UL, 1UL, (unsigned long int)0) returns 1.
  __VERIFIER_assert(add_overflow_p_overflow_9 == 1);


  int add_overflow_p_overflow_10 = __builtin_add_overflow_p(long_long_min, -1LL, long_long_zero);

  // __builtin_add_overflow_p(-9223372036854775808LL, -1LL, (long long int)0) returns 1.
  __VERIFIER_assert(add_overflow_p_overflow_10 == 1);


  int add_overflow_p_overflow_11 = __builtin_add_overflow_p(unsigned_long_long_max, 0ULL, unsigned_long_long_zero);

  // __builtin_add_overflow_p(18446744073709551615ULL, 0ULL, (unsigned long long int)0) returns 0.
  __VERIFIER_assert(add_overflow_p_overflow_11 == 0);


  int add_overflow_p_overflow_12 = __builtin_add_overflow_p(char_max, 1, char_zero);

  // __builtin_add_overflow_p(127, 1, (char)0) or __builtin_add_overflow_p(255, 1, (char)0) returns 1, according to plain-char signedness.
  __VERIFIER_assert(add_overflow_p_overflow_12 == 1);


  // Tests for __builtin_sub_overflow_p.

  const unsigned long int sub_overflow_p_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  __VERIFIER_assert((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)sub_overflow_p_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)sub_overflow_p_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max));


  const long int sub_overflow_p_long_max = (long int)((~0UL) >> 1);

  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  __VERIFIER_assert((sizeof(long int) == sizeof(int) && (int)sub_overflow_p_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)sub_overflow_p_long_max == (long long int)(long int)long_long_max));


  int sub_overflow_p_overflow_1 = __builtin_sub_overflow_p(signed_char_min, 0, signed_char_zero);

  // __builtin_sub_overflow_p(-128, 0, (signed char)0) returns 0.
  __VERIFIER_assert(sub_overflow_p_overflow_1 == 0);


  int sub_overflow_p_overflow_2 = __builtin_sub_overflow_p(signed_char_min, 1, signed_char_zero);

  // __builtin_sub_overflow_p(-128, 1, (signed char)0) returns 1.
  __VERIFIER_assert(sub_overflow_p_overflow_2 == 1);


  int sub_overflow_p_overflow_3 = __builtin_sub_overflow_p(0, 1, unsigned_char_zero);

  // __builtin_sub_overflow_p(0, 1, (unsigned char)0) returns 1.
  __VERIFIER_assert(sub_overflow_p_overflow_3 == 1);


  int sub_overflow_p_overflow_4 = __builtin_sub_overflow_p(short_max, -1, short_zero);

  // __builtin_sub_overflow_p(32767, -1, (short int)0) returns 1.
  __VERIFIER_assert(sub_overflow_p_overflow_4 == 1);


  int sub_overflow_p_overflow_5 = __builtin_sub_overflow_p(unsigned_short_max, unsigned_short_max, unsigned_short_zero);

  // __builtin_sub_overflow_p(65535U, 65535U, (unsigned short int)0) returns 0.
  __VERIFIER_assert(sub_overflow_p_overflow_5 == 0);


  int sub_overflow_p_overflow_6 = __builtin_sub_overflow_p(int_min, 1, int_zero);

  // __builtin_sub_overflow_p((-2147483647 - 1), 1, (int)0) returns 1.
  __VERIFIER_assert(sub_overflow_p_overflow_6 == 1);


  int sub_overflow_p_overflow_7 = __builtin_sub_overflow_p(0U, unsigned_int_max, unsigned_int_zero);

  // __builtin_sub_overflow_p(0U, 4294967295U, (unsigned int)0) returns 1.
  __VERIFIER_assert(sub_overflow_p_overflow_7 == 1);


  int sub_overflow_p_overflow_8 = __builtin_sub_overflow_p(sub_overflow_p_long_max, -1L, long_zero);

  // ILP32: __builtin_sub_overflow_p(2147483647L, -1L, (long int)0) returns 1.
  // LP64: __builtin_sub_overflow_p(9223372036854775807L, -1L, (long int)0) returns 1.
  __VERIFIER_assert(sub_overflow_p_overflow_8 == 1);


  int sub_overflow_p_overflow_9 = __builtin_sub_overflow_p(sub_overflow_p_unsigned_long_max, sub_overflow_p_unsigned_long_max, unsigned_long_zero);

  // ILP32: __builtin_sub_overflow_p(4294967295UL, 4294967295UL, (unsigned long int)0) returns 0.
  // LP64: __builtin_sub_overflow_p(18446744073709551615UL, 18446744073709551615UL, (unsigned long int)0) returns 0.
  __VERIFIER_assert(sub_overflow_p_overflow_9 == 0);


  int sub_overflow_p_overflow_10 = __builtin_sub_overflow_p(0LL, long_long_min, long_long_zero);

  // __builtin_sub_overflow_p(0LL, -9223372036854775808LL, (long long int)0) returns 1.
  __VERIFIER_assert(sub_overflow_p_overflow_10 == 1);


  int sub_overflow_p_overflow_11 = __builtin_sub_overflow_p(unsigned_long_long_max, unsigned_long_long_max, unsigned_long_long_zero);

  // __builtin_sub_overflow_p(18446744073709551615ULL, 18446744073709551615ULL, (unsigned long long int)0) returns 0.
  __VERIFIER_assert(sub_overflow_p_overflow_11 == 0);


  int sub_overflow_p_overflow_12 = __builtin_sub_overflow_p(char_min, 1, char_zero);

  // __builtin_sub_overflow_p(-128, 1, (char)0) or __builtin_sub_overflow_p(0, 1, (char)0) returns 1, according to plain-char signedness.
  __VERIFIER_assert(sub_overflow_p_overflow_12 == 1);


  // Tests for __builtin_mul_overflow_p.

  const unsigned long int mul_overflow_p_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  __VERIFIER_assert((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)mul_overflow_p_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)mul_overflow_p_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max));


  const long int mul_overflow_p_long_max = (long int)((~0UL) >> 1);

  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  __VERIFIER_assert((sizeof(long int) == sizeof(int) && (int)mul_overflow_p_long_max == int_max) ||
         (sizeof(long int) == sizeof(long long int) &&
          (long long int)mul_overflow_p_long_max == (long long int)(long int)long_long_max));


  int mul_overflow_p_overflow_1 = __builtin_mul_overflow_p(signed_char_max, 1, signed_char_zero);

  // __builtin_mul_overflow_p(127, 1, (signed char)0) returns 0.
  __VERIFIER_assert(mul_overflow_p_overflow_1 == 0);


  int mul_overflow_p_overflow_2 = __builtin_mul_overflow_p(signed_char_min, -1, signed_char_zero);

  // __builtin_mul_overflow_p(-128, -1, (signed char)0) returns 1.
  __VERIFIER_assert(mul_overflow_p_overflow_2 == 1);


  int mul_overflow_p_overflow_3 = __builtin_mul_overflow_p(unsigned_char_max, unsigned_char_max, unsigned_char_zero);

  // __builtin_mul_overflow_p(255U, 255U, (unsigned char)0) returns 1.
  __VERIFIER_assert(mul_overflow_p_overflow_3 == 1);


  int mul_overflow_p_overflow_4 = __builtin_mul_overflow_p(short_max, 0, short_zero);

  // __builtin_mul_overflow_p(32767, 0, (short int)0) returns 0.
  __VERIFIER_assert(mul_overflow_p_overflow_4 == 0);


  int mul_overflow_p_overflow_5 = __builtin_mul_overflow_p(2U, 32768U, unsigned_short_zero);

  // __builtin_mul_overflow_p(2U, 65535U / 2U + 1U, (unsigned short int)0) returns 1.
  __VERIFIER_assert(mul_overflow_p_overflow_5 == 1);


  int mul_overflow_p_overflow_6 = __builtin_mul_overflow_p(int_min, -1, int_zero);

  // __builtin_mul_overflow_p((-2147483647 - 1), -1, (int)0) returns 1.
  __VERIFIER_assert(mul_overflow_p_overflow_6 == 1);


  int mul_overflow_p_overflow_7 = __builtin_mul_overflow_p(unsigned_int_max, unsigned_int_max, unsigned_int_zero);

  // __builtin_mul_overflow_p(4294967295U, 4294967295U, (unsigned int)0) returns 1.
  __VERIFIER_assert(mul_overflow_p_overflow_7 == 1);


  int mul_overflow_p_overflow_8 = __builtin_mul_overflow_p(mul_overflow_p_long_max, 0L, long_zero);

  // ILP32: __builtin_mul_overflow_p(2147483647L, 0L, (long int)0) returns 0.
  // LP64: __builtin_mul_overflow_p(9223372036854775807L, 0L, (long int)0) returns 0.
  __VERIFIER_assert(mul_overflow_p_overflow_8 == 0);


  int mul_overflow_p_overflow_9 = __builtin_mul_overflow_p(mul_overflow_p_unsigned_long_max, 2UL, unsigned_long_zero);

  // ILP32: __builtin_mul_overflow_p(4294967295UL, 2UL, (unsigned long int)0) returns 1.
  // LP64: __builtin_mul_overflow_p(18446744073709551615UL, 2UL, (unsigned long int)0) returns 1.
  __VERIFIER_assert(mul_overflow_p_overflow_9 == 1);


  int mul_overflow_p_overflow_10 = __builtin_mul_overflow_p(long_long_min, 1LL, long_long_zero);

  // __builtin_mul_overflow_p(-9223372036854775808LL, 1LL, (long long int)0) returns 0.
  __VERIFIER_assert(mul_overflow_p_overflow_10 == 0);


  int mul_overflow_p_overflow_11 = __builtin_mul_overflow_p(unsigned_long_long_max, unsigned_long_long_max, unsigned_long_long_zero);

  // __builtin_mul_overflow_p(18446744073709551615ULL, 18446744073709551615ULL, (unsigned long long int)0) returns 1.
  __VERIFIER_assert(mul_overflow_p_overflow_11 == 1);


  int mul_overflow_p_overflow_12 = __builtin_mul_overflow_p(char_max, 2, char_zero);

  // __builtin_mul_overflow_p(127, 2, (char)0) or __builtin_mul_overflow_p(255, 2, (char)0) returns 1, according to plain-char signedness.
  __VERIFIER_assert(mul_overflow_p_overflow_12 == 1);
  return 0;
}
