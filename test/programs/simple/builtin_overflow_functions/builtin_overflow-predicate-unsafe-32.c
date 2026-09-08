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


  // This program targets ILP32 and fails its expected verdict under LP64.

  // ILP32: 2147483647L + 1L does not fit in long, so the predicate returns 1.
  int model_add_overflow_p = __builtin_add_overflow_p(2147483647L, 1L, (long int)0);
  all_expected_checks_fail = all_expected_checks_fail || (model_add_overflow_p != 1);

  // Addition overflow predicate tests.

  const unsigned long int add_overflow_p_unsigned_long_max = ~0UL;


  const long int add_overflow_p_long_max = (long int)((~0UL) >> 1);


  int add_overflow_p_overflow_1 = __builtin_add_overflow_p(signed_char_max, 0, signed_char_zero);

  // 127 + 0 = 127, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_1 != 0);


  int add_overflow_p_overflow_2 = __builtin_add_overflow_p(signed_char_max, 1, signed_char_zero);

  // 127 + 1 = 128, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_2 != 1);


  int add_overflow_p_overflow_3 = __builtin_add_overflow_p(unsigned_char_max, 1, unsigned_char_zero);

  // 255U + 1 = 256, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_3 != 1);


  int add_overflow_p_overflow_4 = __builtin_add_overflow_p(short_min, -1, short_zero);

  // -32768 + -1 = -32769, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_4 != 1);


  int add_overflow_p_overflow_5 = __builtin_add_overflow_p(-1, 1U, unsigned_short_zero);

  // -1 + 1U = 0, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_5 != 0);


  int add_overflow_p_overflow_6 = __builtin_add_overflow_p(int_max, 1, int_zero);

  // 2147483647 + 1 = 2147483648, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_6 != 1);


  int add_overflow_p_overflow_7 = __builtin_add_overflow_p(unsigned_int_max, 1U, unsigned_int_zero);

  // 4294967295U + 1U = 4294967296, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_7 != 1);


  int add_overflow_p_overflow_8 = __builtin_add_overflow_p(add_overflow_p_long_max, 0L, long_zero);

  // 2147483647L + 0L = 2147483647, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_8 != 0);


  int add_overflow_p_overflow_9 = __builtin_add_overflow_p(add_overflow_p_unsigned_long_max, 1UL, unsigned_long_zero);

  // 4294967295UL + 1UL = 4294967296, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_9 != 1);


  int add_overflow_p_overflow_10 = __builtin_add_overflow_p(long_long_min, -1LL, long_long_zero);

  // -9223372036854775808LL + -1LL = -9223372036854775809, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_10 != 1);


  int add_overflow_p_overflow_11 = __builtin_add_overflow_p(unsigned_long_long_max, 0ULL, unsigned_long_long_zero);

  // 18446744073709551615ULL + 0ULL = 18446744073709551615, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_11 != 0);


  int add_overflow_p_overflow_12 = __builtin_add_overflow_p(char_max, 1, char_zero);

  // 127 + 1 = 128, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_p_overflow_12 != 1);


  // Subtraction overflow predicate tests.

  const unsigned long int sub_overflow_p_unsigned_long_max = ~0UL;


  const long int sub_overflow_p_long_max = (long int)((~0UL) >> 1);


  int sub_overflow_p_overflow_1 = __builtin_sub_overflow_p(signed_char_min, 0, signed_char_zero);

  // -128 - 0 = -128, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_1 != 0);


  int sub_overflow_p_overflow_2 = __builtin_sub_overflow_p(signed_char_min, 1, signed_char_zero);

  // -128 - 1 = -129, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_2 != 1);


  int sub_overflow_p_overflow_3 = __builtin_sub_overflow_p(0, 1, unsigned_char_zero);

  // 0 - 1 = -1, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_3 != 1);


  int sub_overflow_p_overflow_4 = __builtin_sub_overflow_p(short_max, -1, short_zero);

  // 32767 - -1 = 32768, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_4 != 1);


  int sub_overflow_p_overflow_5 = __builtin_sub_overflow_p(unsigned_short_max, unsigned_short_max, unsigned_short_zero);

  // 65535U - 65535U = 0, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_5 != 0);


  int sub_overflow_p_overflow_6 = __builtin_sub_overflow_p(int_min, 1, int_zero);

  // -2147483648 - 1 = -2147483649, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_6 != 1);


  int sub_overflow_p_overflow_7 = __builtin_sub_overflow_p(0U, unsigned_int_max, unsigned_int_zero);

  // 0U - 4294967295U = -4294967295, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_7 != 1);


  int sub_overflow_p_overflow_8 = __builtin_sub_overflow_p(sub_overflow_p_long_max, -1L, long_zero);

  // 2147483647L - -1L = 2147483648, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_8 != 1);


  int sub_overflow_p_overflow_9 = __builtin_sub_overflow_p(sub_overflow_p_unsigned_long_max, sub_overflow_p_unsigned_long_max, unsigned_long_zero);

  // 4294967295UL - 4294967295UL = 0, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_9 != 0);


  int sub_overflow_p_overflow_10 = __builtin_sub_overflow_p(0LL, long_long_min, long_long_zero);

  // 0LL - -9223372036854775808LL = 9223372036854775808, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_10 != 1);


  int sub_overflow_p_overflow_11 = __builtin_sub_overflow_p(unsigned_long_long_max, unsigned_long_long_max, unsigned_long_long_zero);

  // 18446744073709551615ULL - 18446744073709551615ULL = 0, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_11 != 0);


  int sub_overflow_p_overflow_12 = __builtin_sub_overflow_p(char_min, 1, char_zero);

  // -128 - 1 = -129, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (sub_overflow_p_overflow_12 != 1);


  // Multiplication overflow predicate tests.

  const unsigned long int mul_overflow_p_unsigned_long_max = ~0UL;


  const long int mul_overflow_p_long_max = (long int)((~0UL) >> 1);


  int mul_overflow_p_overflow_1 = __builtin_mul_overflow_p(signed_char_max, 1, signed_char_zero);

  // 127 * 1 = 127, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_1 != 0);


  int mul_overflow_p_overflow_2 = __builtin_mul_overflow_p(signed_char_min, -1, signed_char_zero);

  // -128 * -1 = 128, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_2 != 1);


  int mul_overflow_p_overflow_3 = __builtin_mul_overflow_p(unsigned_char_max, unsigned_char_max, unsigned_char_zero);

  // 255U * 255U = 65025, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_3 != 1);


  int mul_overflow_p_overflow_4 = __builtin_mul_overflow_p(short_max, 0, short_zero);

  // 32767 * 0 = 0, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_4 != 0);


  int mul_overflow_p_overflow_5 = __builtin_mul_overflow_p(2U, 32768U, unsigned_short_zero);

  // 2U * 32768U = 65536, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_5 != 1);


  int mul_overflow_p_overflow_6 = __builtin_mul_overflow_p(int_min, -1, int_zero);

  // -2147483648 * -1 = 2147483648, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_6 != 1);


  int mul_overflow_p_overflow_7 = __builtin_mul_overflow_p(unsigned_int_max, unsigned_int_max, unsigned_int_zero);

  // 4294967295U * 4294967295U = 18446744065119617025, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_7 != 1);


  int mul_overflow_p_overflow_8 = __builtin_mul_overflow_p(mul_overflow_p_long_max, 0L, long_zero);

  // 2147483647L * 0L = 0, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_8 != 0);


  int mul_overflow_p_overflow_9 = __builtin_mul_overflow_p(mul_overflow_p_unsigned_long_max, 2UL, unsigned_long_zero);

  // 4294967295UL * 2UL = 8589934590, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_9 != 1);


  int mul_overflow_p_overflow_10 = __builtin_mul_overflow_p(long_long_min, 1LL, long_long_zero);

  // -9223372036854775808LL * 1LL = -9223372036854775808, which fits the destination range; overflow = 0.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_10 != 0);


  int mul_overflow_p_overflow_11 = __builtin_mul_overflow_p(unsigned_long_long_max, unsigned_long_long_max, unsigned_long_long_zero);

  // 18446744073709551615ULL * 18446744073709551615ULL = 340282366920938463426481119284349108225, which is outside the destination range;
  // overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_11 != 1);


  int mul_overflow_p_overflow_12 = __builtin_mul_overflow_p(char_max, 2, char_zero);

  // 127 * 2 = 254, which is outside the destination range; overflow = 1.
  all_expected_checks_fail = all_expected_checks_fail || (mul_overflow_p_overflow_12 != 1);


  // If no expected-value check fails, every deliberately negated check is false.
  if (!(all_expected_checks_fail))
    goto ERROR;

  return 0;

ERROR:
  return 1;
}
