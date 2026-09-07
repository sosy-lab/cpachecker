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
  int all_expected_checks_fail = 0;


  // Fixed typed test values are initialized directly and never modified.
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


  // Tests for __builtin_add_overflow.

  const unsigned long int add_overflow_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  all_expected_checks_fail = all_expected_checks_fail ||
    (sizeof(unsigned long int) == sizeof(unsigned int) && (unsigned int)add_overflow_unsigned_long_max != unsigned_int_max) ||
    (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
     (unsigned long long int)add_overflow_unsigned_long_max !=
       (unsigned long long int)(unsigned long int)unsigned_long_long_max);


  const long int add_overflow_long_max = (long int)((~0UL) >> 1);

  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  all_expected_checks_fail = all_expected_checks_fail ||
    (sizeof(long int) == sizeof(int) && (int)add_overflow_long_max != int_max) ||
    (sizeof(long int) == sizeof(long long int) &&
     (long long int)add_overflow_long_max != (long long int)(long int)long_long_max);


  const long int add_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);

  // ILP32: The calculated long minimum must equal int minimum -2147483648.
  // LP64: The calculated long minimum must equal long long minimum -9223372036854775808.
  all_expected_checks_fail = all_expected_checks_fail ||
    (sizeof(long int) == sizeof(int) && (int)add_overflow_long_min != int_min) ||
    (sizeof(long int) == sizeof(long long int) &&
     (long long int)add_overflow_long_min !=
       (long long int)(long int)long_long_min);


  signed char add_overflow_result_1;
  int add_overflow_overflow_1 = __builtin_add_overflow(signed_char_max, 0, &add_overflow_result_1);

  // __builtin_add_overflow(127, 0, &add_overflow_result_1) stores 127.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_1 != signed_char_max);

  // __builtin_add_overflow(127, 0, &add_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_1 != 0);


  signed char add_overflow_result_2;
  int add_overflow_overflow_2 = __builtin_add_overflow(signed_char_max, 1, &add_overflow_result_2);

  // __builtin_add_overflow(127, 1, &add_overflow_result_2) stores -128.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_2 != signed_char_min);

  // __builtin_add_overflow(127, 1, &add_overflow_result_2) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_2 != 1);


  unsigned char add_overflow_result_3;
  int add_overflow_overflow_3 = __builtin_add_overflow(unsigned_char_max, 1, &add_overflow_result_3);

  // __builtin_add_overflow(255U, 1, &add_overflow_result_3) stores 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_3 != 0);

  // __builtin_add_overflow(255U, 1, &add_overflow_result_3) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_3 != 1);


  short int add_overflow_result_4;
  int add_overflow_overflow_4 = __builtin_add_overflow(short_min, -1, &add_overflow_result_4);

  // __builtin_add_overflow(-32768, -1, &add_overflow_result_4) stores 32767.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_4 != short_max);

  // __builtin_add_overflow(-32768, -1, &add_overflow_result_4) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_4 != 1);


  unsigned short int add_overflow_result_5;
  int add_overflow_overflow_5 = __builtin_add_overflow(-1, 1U, &add_overflow_result_5);

  // __builtin_add_overflow(-1, 1U, &add_overflow_result_5) stores 0U.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_5 != 0U);

  // __builtin_add_overflow(-1, 1U, &add_overflow_result_5) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_5 != 0);


  int add_overflow_result_6;
  int add_overflow_overflow_6 = __builtin_add_overflow(int_max, 1, &add_overflow_result_6);

  // __builtin_add_overflow(2147483647, 1, &add_overflow_result_6) stores (-2147483647 - 1).
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_6 != int_min);

  // __builtin_add_overflow(2147483647, 1, &add_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_6 != 1);


  unsigned int add_overflow_result_7;
  int add_overflow_overflow_7 = __builtin_add_overflow(unsigned_int_max, 1U, &add_overflow_result_7);

  // __builtin_add_overflow(4294967295U, 1U, &add_overflow_result_7) stores 0U.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_7 != 0U);

  // __builtin_add_overflow(4294967295U, 1U, &add_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_7 != 1);


  long int add_overflow_result_8;
  int add_overflow_overflow_8 = __builtin_add_overflow(add_overflow_long_max, 1L, &add_overflow_result_8);

  // ILP32: __builtin_add_overflow(2147483647L, 1L, &add_overflow_result_8) stores -2147483648L.
  // LP64: __builtin_add_overflow(9223372036854775807L, 1L, &add_overflow_result_8) stores -9223372036854775808L.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_8 != add_overflow_long_min);

  // ILP32: __builtin_add_overflow(2147483647L, 1L, &add_overflow_result_8) returns 1.
  // LP64: __builtin_add_overflow(9223372036854775807L, 1L, &add_overflow_result_8) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_8 != 1);


  unsigned long int add_overflow_result_9;
  int add_overflow_overflow_9 = __builtin_add_overflow(add_overflow_unsigned_long_max, 1UL, &add_overflow_result_9);

  // ILP32: __builtin_add_overflow(4294967295UL, 1UL, &add_overflow_result_9) stores 0UL.
  // LP64: __builtin_add_overflow(18446744073709551615UL, 1UL, &add_overflow_result_9) stores 0UL.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_9 != 0UL);

  // ILP32: __builtin_add_overflow(4294967295UL, 1UL, &add_overflow_result_9) returns 1.
  // LP64: __builtin_add_overflow(18446744073709551615UL, 1UL, &add_overflow_result_9) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_9 != 1);


  long long int add_overflow_result_10;
  int add_overflow_overflow_10 = __builtin_add_overflow(long_long_min, -1LL, &add_overflow_result_10);

  // __builtin_add_overflow(-9223372036854775808LL, -1LL, &add_overflow_result_10) stores 9223372036854775807LL.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_10 != long_long_max);

  // __builtin_add_overflow(-9223372036854775808LL, -1LL, &add_overflow_result_10) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_10 != 1);


  unsigned long long int add_overflow_result_11;
  int add_overflow_overflow_11 = __builtin_add_overflow(unsigned_long_long_max, 1ULL, &add_overflow_result_11);

  // __builtin_add_overflow(18446744073709551615ULL, 1ULL, &add_overflow_result_11) stores 0ULL.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_11 != 0ULL);

  // __builtin_add_overflow(18446744073709551615ULL, 1ULL, &add_overflow_result_11) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_11 != 1);


  char add_overflow_result_12;
  int add_overflow_overflow_12 = __builtin_add_overflow(char_max, 1, &add_overflow_result_12);

  // __builtin_add_overflow((char)127, 1, &add_overflow_result_12) stores (char)-128.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_12 != char_min);

  // __builtin_add_overflow((char)127, 1, &add_overflow_result_12) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_12 != 1);


  int add_overflow_result_13;
  int add_overflow_overflow_13 = __builtin_add_overflow(-1LL, 1ULL, &add_overflow_result_13);

  // __builtin_add_overflow(-1LL, 1ULL, &add_overflow_result_13) stores 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_13 != 0);

  // __builtin_add_overflow(-1LL, 1ULL, &add_overflow_result_13) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_13 != 0);


  int add_overflow_result_14;
  int add_overflow_overflow_14 = __builtin_add_overflow(unsigned_long_long_max, 0, &add_overflow_result_14);

  // __builtin_add_overflow(18446744073709551615ULL, 0, &add_overflow_result_14) stores (int)18446744073709551615ULL.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_result_14 != (int)unsigned_long_long_max);

  // __builtin_add_overflow(18446744073709551615ULL, 0, &add_overflow_result_14) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (add_overflow_overflow_14 != 1);


  // Tests for __builtin_sadd_overflow.

  int sadd_overflow_result_1;
  int sadd_overflow_overflow_1 = __builtin_sadd_overflow(0, 0, &sadd_overflow_result_1);

  // __builtin_sadd_overflow(0, 0, &sadd_overflow_result_1) stores 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_1 != 0);

  // __builtin_sadd_overflow(0, 0, &sadd_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_1 != 0);


  int sadd_overflow_result_2;
  int sadd_overflow_overflow_2 = __builtin_sadd_overflow(int_max, 0, &sadd_overflow_result_2);

  // __builtin_sadd_overflow(2147483647, 0, &sadd_overflow_result_2) stores 2147483647.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_2 != int_max);

  // __builtin_sadd_overflow(2147483647, 0, &sadd_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_2 != 0);


  int sadd_overflow_result_3;
  int sadd_overflow_overflow_3 = __builtin_sadd_overflow(int_min, 0, &sadd_overflow_result_3);

  // __builtin_sadd_overflow((-2147483647 - 1), 0, &sadd_overflow_result_3) stores (-2147483647 - 1).
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_3 != int_min);

  // __builtin_sadd_overflow((-2147483647 - 1), 0, &sadd_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_3 != 0);


  int sadd_overflow_result_4;
  int sadd_overflow_overflow_4 = __builtin_sadd_overflow(int_max, -1, &sadd_overflow_result_4);

  // __builtin_sadd_overflow(2147483647, -1, &sadd_overflow_result_4) stores 2147483647 - 1.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_4 != 2147483646);

  // __builtin_sadd_overflow(2147483647, -1, &sadd_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_4 != 0);


  int sadd_overflow_result_5;
  int sadd_overflow_overflow_5 = __builtin_sadd_overflow(int_min, 1, &sadd_overflow_result_5);

  // __builtin_sadd_overflow((-2147483647 - 1), 1, &sadd_overflow_result_5) stores (-2147483647 - 1) + 1.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_5 != -int_max);

  // __builtin_sadd_overflow((-2147483647 - 1), 1, &sadd_overflow_result_5) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_5 != 0);


  int sadd_overflow_result_6;
  int sadd_overflow_overflow_6 = __builtin_sadd_overflow(int_max, 1, &sadd_overflow_result_6);

  // __builtin_sadd_overflow(2147483647, 1, &sadd_overflow_result_6) stores (-2147483647 - 1).
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_6 != int_min);

  // __builtin_sadd_overflow(2147483647, 1, &sadd_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_6 != 1);


  int sadd_overflow_result_7;
  int sadd_overflow_overflow_7 = __builtin_sadd_overflow(int_min, -1, &sadd_overflow_result_7);

  // __builtin_sadd_overflow((-2147483647 - 1), -1, &sadd_overflow_result_7) stores 2147483647.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_result_7 != int_max);

  // __builtin_sadd_overflow((-2147483647 - 1), -1, &sadd_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (sadd_overflow_overflow_7 != 1);


  // Tests for __builtin_saddl_overflow.

  const unsigned long int saddl_overflow_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  all_expected_checks_fail = all_expected_checks_fail ||
    (sizeof(unsigned long int) == sizeof(unsigned int) && (unsigned int)saddl_overflow_unsigned_long_max != unsigned_int_max) ||
    (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
     (unsigned long long int)saddl_overflow_unsigned_long_max !=
       (unsigned long long int)(unsigned long int)unsigned_long_long_max);


  const long int saddl_overflow_long_max = (long int)((~0UL) >> 1);

  // ILP32: The calculated long maximum must equal int maximum 2147483647.
  // LP64: The calculated long maximum must equal long long maximum 9223372036854775807LL.
  all_expected_checks_fail = all_expected_checks_fail ||
    (sizeof(long int) == sizeof(int) && (int)saddl_overflow_long_max != int_max) ||
    (sizeof(long int) == sizeof(long long int) &&
     (long long int)saddl_overflow_long_max != (long long int)(long int)long_long_max);


  const long int saddl_overflow_long_min = (-((long int)((~0UL) >> 1)) - 1L);

  // ILP32: The calculated long minimum must equal int minimum -2147483648.
  // LP64: The calculated long minimum must equal long long minimum -9223372036854775808.
  all_expected_checks_fail = all_expected_checks_fail ||
    (sizeof(long int) == sizeof(int) && (int)saddl_overflow_long_min != int_min) ||
    (sizeof(long int) == sizeof(long long int) &&
     (long long int)saddl_overflow_long_min !=
       (long long int)(long int)long_long_min);


  long int saddl_overflow_result_1;
  int saddl_overflow_overflow_1 = __builtin_saddl_overflow(0L, 0L, &saddl_overflow_result_1);

  // __builtin_saddl_overflow(0L, 0L, &saddl_overflow_result_1) stores 0L.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_1 != 0L);

  // __builtin_saddl_overflow(0L, 0L, &saddl_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_1 != 0);


  long int saddl_overflow_result_2;
  int saddl_overflow_overflow_2 = __builtin_saddl_overflow(saddl_overflow_long_max, 0L, &saddl_overflow_result_2);

  // ILP32: __builtin_saddl_overflow(2147483647L, 0L, &saddl_overflow_result_2) stores 2147483647L.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, 0L, &saddl_overflow_result_2) stores 9223372036854775807L.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_2 != saddl_overflow_long_max);

  // ILP32: __builtin_saddl_overflow(2147483647L, 0L, &saddl_overflow_result_2) returns 0.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, 0L, &saddl_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_2 != 0);


  long int saddl_overflow_result_3;
  int saddl_overflow_overflow_3 = __builtin_saddl_overflow(saddl_overflow_long_min, 0L, &saddl_overflow_result_3);

  // ILP32: __builtin_saddl_overflow(-2147483648L, 0L, &saddl_overflow_result_3) stores -2147483648L.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, 0L, &saddl_overflow_result_3) stores -9223372036854775808L.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_3 != saddl_overflow_long_min);

  // ILP32: __builtin_saddl_overflow(-2147483648L, 0L, &saddl_overflow_result_3) returns 0.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, 0L, &saddl_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_3 != 0);


  long int saddl_overflow_result_4;
  int saddl_overflow_overflow_4 = __builtin_saddl_overflow(saddl_overflow_long_max, -1L, &saddl_overflow_result_4);

  // ILP32: __builtin_saddl_overflow(2147483647L, -1L, &saddl_overflow_result_4) stores 2147483647L - 1L.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, -1L, &saddl_overflow_result_4) stores 9223372036854775807L - 1L.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_4 != ((long int)((~0UL) >> 1)) - 1L);

  // ILP32: __builtin_saddl_overflow(2147483647L, -1L, &saddl_overflow_result_4) returns 0.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, -1L, &saddl_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_4 != 0);


  long int saddl_overflow_result_5;
  int saddl_overflow_overflow_5 = __builtin_saddl_overflow(saddl_overflow_long_min, 1L, &saddl_overflow_result_5);

  // ILP32: __builtin_saddl_overflow(-2147483648L, 1L, &saddl_overflow_result_5) stores -2147483648L + 1L.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, 1L, &saddl_overflow_result_5) stores -9223372036854775808L + 1L.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_5 != ((-((long int)((~0UL) >> 1)) - 1L)) + 1L);

  // ILP32: __builtin_saddl_overflow(-2147483648L, 1L, &saddl_overflow_result_5) returns 0.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, 1L, &saddl_overflow_result_5) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_5 != 0);


  long int saddl_overflow_result_6;
  int saddl_overflow_overflow_6 = __builtin_saddl_overflow(saddl_overflow_long_max, 1L, &saddl_overflow_result_6);

  // ILP32: __builtin_saddl_overflow(2147483647L, 1L, &saddl_overflow_result_6) stores -2147483648L.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, 1L, &saddl_overflow_result_6) stores -9223372036854775808L.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_6 != saddl_overflow_long_min);

  // ILP32: __builtin_saddl_overflow(2147483647L, 1L, &saddl_overflow_result_6) returns 1.
  // LP64: __builtin_saddl_overflow(9223372036854775807L, 1L, &saddl_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_6 != 1);


  long int saddl_overflow_result_7;
  int saddl_overflow_overflow_7 = __builtin_saddl_overflow(saddl_overflow_long_min, -1L, &saddl_overflow_result_7);

  // ILP32: __builtin_saddl_overflow(-2147483648L, -1L, &saddl_overflow_result_7) stores 2147483647L.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, -1L, &saddl_overflow_result_7) stores 9223372036854775807L.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_result_7 != saddl_overflow_long_max);

  // ILP32: __builtin_saddl_overflow(-2147483648L, -1L, &saddl_overflow_result_7) returns 1.
  // LP64: __builtin_saddl_overflow(-9223372036854775808L, -1L, &saddl_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddl_overflow_overflow_7 != 1);


  // Tests for __builtin_saddll_overflow.

  long long int saddll_overflow_result_1;
  int saddll_overflow_overflow_1 = __builtin_saddll_overflow(0LL, 0LL, &saddll_overflow_result_1);

  // __builtin_saddll_overflow(0LL, 0LL, &saddll_overflow_result_1) stores 0LL.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_1 != 0LL);

  // __builtin_saddll_overflow(0LL, 0LL, &saddll_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_1 != 0);


  long long int saddll_overflow_result_2;
  int saddll_overflow_overflow_2 = __builtin_saddll_overflow(long_long_max, 0LL, &saddll_overflow_result_2);

  // __builtin_saddll_overflow(9223372036854775807LL, 0LL, &saddll_overflow_result_2) stores 9223372036854775807LL.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_2 != long_long_max);

  // __builtin_saddll_overflow(9223372036854775807LL, 0LL, &saddll_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_2 != 0);


  long long int saddll_overflow_result_3;
  int saddll_overflow_overflow_3 = __builtin_saddll_overflow(long_long_min, 0LL, &saddll_overflow_result_3);

  // __builtin_saddll_overflow(-9223372036854775808LL, 0LL, &saddll_overflow_result_3) stores -9223372036854775808LL.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_3 != long_long_min);

  // __builtin_saddll_overflow(-9223372036854775808LL, 0LL, &saddll_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_3 != 0);


  long long int saddll_overflow_result_4;
  int saddll_overflow_overflow_4 = __builtin_saddll_overflow(long_long_max, -1LL, &saddll_overflow_result_4);

  // __builtin_saddll_overflow(9223372036854775807LL, -1LL, &saddll_overflow_result_4) stores 9223372036854775807LL - 1LL.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_4 != 9223372036854775806LL);

  // __builtin_saddll_overflow(9223372036854775807LL, -1LL, &saddll_overflow_result_4) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_4 != 0);


  long long int saddll_overflow_result_5;
  int saddll_overflow_overflow_5 = __builtin_saddll_overflow(long_long_min, 1LL, &saddll_overflow_result_5);

  // __builtin_saddll_overflow(-9223372036854775808LL, 1LL, &saddll_overflow_result_5) stores -9223372036854775808LL + 1LL.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_5 != -long_long_max);

  // __builtin_saddll_overflow(-9223372036854775808LL, 1LL, &saddll_overflow_result_5) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_5 != 0);


  long long int saddll_overflow_result_6;
  int saddll_overflow_overflow_6 = __builtin_saddll_overflow(long_long_max, 1LL, &saddll_overflow_result_6);

  // __builtin_saddll_overflow(9223372036854775807LL, 1LL, &saddll_overflow_result_6) stores -9223372036854775808LL.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_6 != long_long_min);

  // __builtin_saddll_overflow(9223372036854775807LL, 1LL, &saddll_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_6 != 1);


  long long int saddll_overflow_result_7;
  int saddll_overflow_overflow_7 = __builtin_saddll_overflow(long_long_min, -1LL, &saddll_overflow_result_7);

  // __builtin_saddll_overflow(-9223372036854775808LL, -1LL, &saddll_overflow_result_7) stores 9223372036854775807LL.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_result_7 != long_long_max);

  // __builtin_saddll_overflow(-9223372036854775808LL, -1LL, &saddll_overflow_result_7) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (saddll_overflow_overflow_7 != 1);


  // Tests for __builtin_uadd_overflow.

  unsigned int uadd_overflow_result_1;
  int uadd_overflow_overflow_1 = __builtin_uadd_overflow(0U, 0U, &uadd_overflow_result_1);

  // __builtin_uadd_overflow(0U, 0U, &uadd_overflow_result_1) stores 0U.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_1 != 0U);

  // __builtin_uadd_overflow(0U, 0U, &uadd_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_1 != 0);


  unsigned int uadd_overflow_result_2;
  int uadd_overflow_overflow_2 = __builtin_uadd_overflow(unsigned_int_max, 0U, &uadd_overflow_result_2);

  // __builtin_uadd_overflow(4294967295U, 0U, &uadd_overflow_result_2) stores 4294967295U.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_2 != unsigned_int_max);

  // __builtin_uadd_overflow(4294967295U, 0U, &uadd_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_2 != 0);


  unsigned int uadd_overflow_result_3;
  int uadd_overflow_overflow_3 = __builtin_uadd_overflow(4294967294U, 1U, &uadd_overflow_result_3);

  // __builtin_uadd_overflow(4294967295U - 1U, 1U, &uadd_overflow_result_3) stores 4294967295U.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_3 != unsigned_int_max);

  // __builtin_uadd_overflow(4294967295U - 1U, 1U, &uadd_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_3 != 0);


  unsigned int uadd_overflow_result_4;
  int uadd_overflow_overflow_4 = __builtin_uadd_overflow(unsigned_int_max, 1U, &uadd_overflow_result_4);

  // __builtin_uadd_overflow(4294967295U, 1U, &uadd_overflow_result_4) stores 0U.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_4 != 0U);

  // __builtin_uadd_overflow(4294967295U, 1U, &uadd_overflow_result_4) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_4 != 1);


  unsigned int uadd_overflow_result_5;
  int uadd_overflow_overflow_5 = __builtin_uadd_overflow(unsigned_int_max, 2U, &uadd_overflow_result_5);

  // __builtin_uadd_overflow(4294967295U, 2U, &uadd_overflow_result_5) stores 1U.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_5 != 1U);

  // __builtin_uadd_overflow(4294967295U, 2U, &uadd_overflow_result_5) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_5 != 1);


  unsigned int uadd_overflow_result_6;
  int uadd_overflow_overflow_6 = __builtin_uadd_overflow(unsigned_int_max, unsigned_int_max, &uadd_overflow_result_6);

  // __builtin_uadd_overflow(4294967295U, 4294967295U, &uadd_overflow_result_6) stores 4294967295U - 1U.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_result_6 != 4294967294U);

  // __builtin_uadd_overflow(4294967295U, 4294967295U, &uadd_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (uadd_overflow_overflow_6 != 1);


  // Tests for __builtin_uaddl_overflow.

  const unsigned long int uaddl_overflow_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  all_expected_checks_fail = all_expected_checks_fail ||
    (sizeof(unsigned long int) == sizeof(unsigned int) && (unsigned int)uaddl_overflow_unsigned_long_max != unsigned_int_max) ||
    (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
     (unsigned long long int)uaddl_overflow_unsigned_long_max !=
       (unsigned long long int)(unsigned long int)unsigned_long_long_max);


  unsigned long int uaddl_overflow_result_1;
  int uaddl_overflow_overflow_1 = __builtin_uaddl_overflow(0UL, 0UL, &uaddl_overflow_result_1);

  // __builtin_uaddl_overflow(0UL, 0UL, &uaddl_overflow_result_1) stores 0UL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_1 != 0UL);

  // __builtin_uaddl_overflow(0UL, 0UL, &uaddl_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_1 != 0);


  unsigned long int uaddl_overflow_result_2;
  int uaddl_overflow_overflow_2 = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 0UL, &uaddl_overflow_result_2);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 0UL, &uaddl_overflow_result_2) stores 4294967295UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 0UL, &uaddl_overflow_result_2) stores 18446744073709551615UL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_2 != uaddl_overflow_unsigned_long_max);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 0UL, &uaddl_overflow_result_2) returns 0.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 0UL, &uaddl_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_2 != 0);


  unsigned long int uaddl_overflow_result_3;
  int uaddl_overflow_overflow_3 = __builtin_uaddl_overflow((~0UL) - 1UL, 1UL, &uaddl_overflow_result_3);

  // ILP32: __builtin_uaddl_overflow(4294967294UL, 1UL, &uaddl_overflow_result_3) stores 4294967295UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551614UL, 1UL, &uaddl_overflow_result_3) stores 18446744073709551615UL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_3 != uaddl_overflow_unsigned_long_max);

  // ILP32: __builtin_uaddl_overflow(4294967294UL, 1UL, &uaddl_overflow_result_3) returns 0.
  // LP64: __builtin_uaddl_overflow(18446744073709551614UL, 1UL, &uaddl_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_3 != 0);


  unsigned long int uaddl_overflow_result_4;
  int uaddl_overflow_overflow_4 = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 1UL, &uaddl_overflow_result_4);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 1UL, &uaddl_overflow_result_4) stores 0UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 1UL, &uaddl_overflow_result_4) stores 0UL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_4 != 0UL);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 1UL, &uaddl_overflow_result_4) returns 1.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 1UL, &uaddl_overflow_result_4) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_4 != 1);


  unsigned long int uaddl_overflow_result_5;
  int uaddl_overflow_overflow_5 = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, 2UL, &uaddl_overflow_result_5);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 2UL, &uaddl_overflow_result_5) stores 1UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 2UL, &uaddl_overflow_result_5) stores 1UL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_5 != 1UL);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 2UL, &uaddl_overflow_result_5) returns 1.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 2UL, &uaddl_overflow_result_5) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_5 != 1);


  unsigned long int uaddl_overflow_result_6;
  int uaddl_overflow_overflow_6 = __builtin_uaddl_overflow(uaddl_overflow_unsigned_long_max, uaddl_overflow_unsigned_long_max, &uaddl_overflow_result_6);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 4294967295UL, &uaddl_overflow_result_6) stores 4294967294UL.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 18446744073709551615UL, &uaddl_overflow_result_6) stores 18446744073709551614UL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_result_6 != (~0UL) - 1UL);

  // ILP32: __builtin_uaddl_overflow(4294967295UL, 4294967295UL, &uaddl_overflow_result_6) returns 1.
  // LP64: __builtin_uaddl_overflow(18446744073709551615UL, 18446744073709551615UL, &uaddl_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddl_overflow_overflow_6 != 1);


  // Tests for __builtin_uaddll_overflow.

  unsigned long long int uaddll_overflow_result_1;
  int uaddll_overflow_overflow_1 = __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_overflow_result_1);

  // __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_overflow_result_1) stores 0ULL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_1 != 0ULL);

  // __builtin_uaddll_overflow(0ULL, 0ULL, &uaddll_overflow_result_1) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_1 != 0);


  unsigned long long int uaddll_overflow_result_2;
  int uaddll_overflow_overflow_2 = __builtin_uaddll_overflow(unsigned_long_long_max, 0ULL, &uaddll_overflow_result_2);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 0ULL, &uaddll_overflow_result_2) stores 18446744073709551615ULL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_2 != unsigned_long_long_max);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 0ULL, &uaddll_overflow_result_2) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_2 != 0);


  unsigned long long int uaddll_overflow_result_3;
  int uaddll_overflow_overflow_3 = __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_overflow_result_3);

  // __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_overflow_result_3) stores 18446744073709551615ULL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_3 != unsigned_long_long_max);

  // __builtin_uaddll_overflow(18446744073709551614ULL, 1ULL, &uaddll_overflow_result_3) returns 0.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_3 != 0);


  unsigned long long int uaddll_overflow_result_4;
  int uaddll_overflow_overflow_4 = __builtin_uaddll_overflow(unsigned_long_long_max, 1ULL, &uaddll_overflow_result_4);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 1ULL, &uaddll_overflow_result_4) stores 0ULL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_4 != 0ULL);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 1ULL, &uaddll_overflow_result_4) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_4 != 1);


  unsigned long long int uaddll_overflow_result_5;
  int uaddll_overflow_overflow_5 = __builtin_uaddll_overflow(unsigned_long_long_max, 2ULL, &uaddll_overflow_result_5);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 2ULL, &uaddll_overflow_result_5) stores 1ULL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_5 != 1ULL);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 2ULL, &uaddll_overflow_result_5) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_5 != 1);


  unsigned long long int uaddll_overflow_result_6;
  int uaddll_overflow_overflow_6 = __builtin_uaddll_overflow(unsigned_long_long_max, unsigned_long_long_max, &uaddll_overflow_result_6);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 18446744073709551615ULL, &uaddll_overflow_result_6) stores 18446744073709551614ULL.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_result_6 != 18446744073709551614ULL);

  // __builtin_uaddll_overflow(18446744073709551615ULL, 18446744073709551615ULL, &uaddll_overflow_result_6) returns 1.
  all_expected_checks_fail = all_expected_checks_fail || (uaddll_overflow_overflow_6 != 1);


  // If no expected-value check fails, every deliberately negated check is false.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
