// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __VERIFIER_assert(int pCondition) {
  if (!pCondition) {
  ERROR:
    goto ERROR;
  }
}

// ILP32/LP64: safe; ErrorLabel asserts all allocated integer values stay in range.
int main(void) {
  const signed char signed_char_min = -128;
  const signed char signed_char_max = 127;
  const unsigned char unsigned_char_max = 255U;
  const short short_min = -32767 - 1;
  const short short_max = 32767;
  const unsigned short unsigned_short_max = 65535U;
  const int int_min = -2147483647 - 1;
  const int int_max = 2147483647;
  const unsigned int unsigned_int_max = 4294967295U;
  // Long limits vary with the machine model.
  const long long long_min =
      sizeof(long) == 4U ? -2147483647LL - 1LL : -9223372036854775807LL - 1LL;
  const long long long_max = sizeof(long) == 4U ? 2147483647LL : 9223372036854775807LL;
  const unsigned long long unsigned_long_max =
      sizeof(unsigned long) == 4U ? 4294967295ULL : 18446744073709551615ULL;
  const long long long_long_min = -9223372036854775807LL - 1LL;
  const long long long_long_max = 9223372036854775807LL;
  const unsigned long long unsigned_long_long_max = 18446744073709551615ULL;

  // Each allocation holds the two boundary values of its type.
  signed char *signed_char_values = __builtin_alloca(2 * sizeof(*signed_char_values));
  unsigned char *unsigned_char_values = __builtin_alloca(2 * sizeof(*unsigned_char_values));
  short *short_values = __builtin_alloca(2 * sizeof(*short_values));
  unsigned short *unsigned_short_values = __builtin_alloca(2 * sizeof(*unsigned_short_values));
  int *int_values = __builtin_alloca(2 * sizeof(*int_values));
  unsigned int *unsigned_int_values = __builtin_alloca(2 * sizeof(*unsigned_int_values));
  long *long_values = __builtin_alloca(2 * sizeof(*long_values));
  unsigned long *unsigned_long_values = __builtin_alloca(2 * sizeof(*unsigned_long_values));
  long long *long_long_values = __builtin_alloca(2 * sizeof(*long_long_values));
  unsigned long long *unsigned_long_long_values =
      __builtin_alloca(2 * sizeof(*unsigned_long_long_values));

  // Signed char: approach and restore both limits without overflow.
  *signed_char_values = signed_char_max;
  signed_char_values[1] = signed_char_min;
  __VERIFIER_assert(*signed_char_values == signed_char_max);
  __VERIFIER_assert(signed_char_values[1] == signed_char_min);
  *signed_char_values = (signed char)(*signed_char_values - 1);
  signed_char_values[1] = (signed char)(signed_char_values[1] + 1);
  __VERIFIER_assert(*signed_char_values == signed_char_max - 1);
  __VERIFIER_assert(signed_char_values[1] == signed_char_min + 1);
  *signed_char_values = (signed char)(*signed_char_values + 1);
  signed_char_values[1] = (signed char)(signed_char_values[1] - 1);
  __VERIFIER_assert(*signed_char_values == signed_char_max);
  __VERIFIER_assert(signed_char_values[1] == signed_char_min);

  // Unsigned char: wrap around without changing the adjacent element.
  *unsigned_char_values = unsigned_char_max;
  unsigned_char_values[1] = unsigned_char_max;
  __VERIFIER_assert(*unsigned_char_values == unsigned_char_max);
  __VERIFIER_assert(unsigned_char_values[1] == unsigned_char_max);
  *unsigned_char_values = (unsigned char)(*unsigned_char_values + 1U);
  __VERIFIER_assert(*unsigned_char_values == 0U);
  __VERIFIER_assert(unsigned_char_values[1] == unsigned_char_max);
  *unsigned_char_values = (unsigned char)(*unsigned_char_values - 1U);
  __VERIFIER_assert(*unsigned_char_values == unsigned_char_max);

  // Signed short: approach and restore both limits without overflow.
  *short_values = short_max;
  short_values[1] = short_min;
  __VERIFIER_assert(*short_values == short_max);
  __VERIFIER_assert(short_values[1] == short_min);
  *short_values = (short)(*short_values - 1);
  short_values[1] = (short)(short_values[1] + 1);
  __VERIFIER_assert(*short_values == short_max - 1);
  __VERIFIER_assert(short_values[1] == short_min + 1);
  *short_values = (short)(*short_values + 1);
  short_values[1] = (short)(short_values[1] - 1);
  __VERIFIER_assert(*short_values == short_max);
  __VERIFIER_assert(short_values[1] == short_min);

  // Unsigned short: wrap around without changing the adjacent element.
  *unsigned_short_values = unsigned_short_max;
  unsigned_short_values[1] = unsigned_short_max;
  __VERIFIER_assert(*unsigned_short_values == unsigned_short_max);
  __VERIFIER_assert(unsigned_short_values[1] == unsigned_short_max);
  *unsigned_short_values = (unsigned short)(*unsigned_short_values + 1U);
  __VERIFIER_assert(*unsigned_short_values == 0U);
  __VERIFIER_assert(unsigned_short_values[1] == unsigned_short_max);
  *unsigned_short_values = (unsigned short)(*unsigned_short_values - 1U);
  __VERIFIER_assert(*unsigned_short_values == unsigned_short_max);

  // Signed int: approach and restore both limits without overflow.
  *int_values = int_max;
  int_values[1] = int_min;
  __VERIFIER_assert(*int_values == int_max);
  __VERIFIER_assert(int_values[1] == int_min);
  *int_values = *int_values - 1;
  int_values[1] = int_values[1] + 1;
  __VERIFIER_assert(*int_values == int_max - 1);
  __VERIFIER_assert(int_values[1] == int_min + 1);
  *int_values = *int_values + 1;
  int_values[1] = int_values[1] - 1;
  __VERIFIER_assert(*int_values == int_max);
  __VERIFIER_assert(int_values[1] == int_min);

  // Unsigned int: wrap around without changing the adjacent element.
  *unsigned_int_values = unsigned_int_max;
  unsigned_int_values[1] = unsigned_int_max;
  __VERIFIER_assert(*unsigned_int_values == unsigned_int_max);
  __VERIFIER_assert(unsigned_int_values[1] == unsigned_int_max);
  *unsigned_int_values = *unsigned_int_values + 1U;
  __VERIFIER_assert(*unsigned_int_values == 0U);
  __VERIFIER_assert(unsigned_int_values[1] == unsigned_int_max);
  *unsigned_int_values = *unsigned_int_values - 1U;
  __VERIFIER_assert(*unsigned_int_values == unsigned_int_max);

  // Signed long: approach and restore both model-specific limits without overflow.
  *long_values = (long)long_max;
  long_values[1] = (long)long_min;
  __VERIFIER_assert((long long)*long_values == long_max);
  __VERIFIER_assert((long long)long_values[1] == long_min);
  *long_values = *long_values - 1L;
  long_values[1] = long_values[1] + 1L;
  __VERIFIER_assert((long long)*long_values == long_max - 1LL);
  __VERIFIER_assert((long long)long_values[1] == long_min + 1LL);
  *long_values = *long_values + 1L;
  long_values[1] = long_values[1] - 1L;
  __VERIFIER_assert((long long)*long_values == long_max);
  __VERIFIER_assert((long long)long_values[1] == long_min);

  // Unsigned long: wrap around without changing the adjacent element.
  *unsigned_long_values = (unsigned long)unsigned_long_max;
  unsigned_long_values[1] = (unsigned long)unsigned_long_max;
  __VERIFIER_assert((unsigned long long)*unsigned_long_values == unsigned_long_max);
  __VERIFIER_assert((unsigned long long)unsigned_long_values[1] == unsigned_long_max);
  *unsigned_long_values = *unsigned_long_values + 1UL;
  __VERIFIER_assert(*unsigned_long_values == 0UL);
  __VERIFIER_assert((unsigned long long)unsigned_long_values[1] == unsigned_long_max);
  *unsigned_long_values = *unsigned_long_values - 1UL;
  __VERIFIER_assert((unsigned long long)*unsigned_long_values == unsigned_long_max);

  // Signed long long: approach and restore both limits without overflow.
  *long_long_values = long_long_max;
  long_long_values[1] = long_long_min;
  __VERIFIER_assert(*long_long_values == long_long_max);
  __VERIFIER_assert(long_long_values[1] == long_long_min);
  *long_long_values = *long_long_values - 1LL;
  long_long_values[1] = long_long_values[1] + 1LL;
  __VERIFIER_assert(*long_long_values == long_long_max - 1LL);
  __VERIFIER_assert(long_long_values[1] == long_long_min + 1LL);
  *long_long_values = *long_long_values + 1LL;
  long_long_values[1] = long_long_values[1] - 1LL;
  __VERIFIER_assert(*long_long_values == long_long_max);
  __VERIFIER_assert(long_long_values[1] == long_long_min);

  // Unsigned long long: wrap around without changing the adjacent element.
  *unsigned_long_long_values = unsigned_long_long_max;
  unsigned_long_long_values[1] = unsigned_long_long_max;
  __VERIFIER_assert(*unsigned_long_long_values == unsigned_long_long_max);
  __VERIFIER_assert(unsigned_long_long_values[1] == unsigned_long_long_max);
  *unsigned_long_long_values = *unsigned_long_long_values + 1ULL;
  __VERIFIER_assert(*unsigned_long_long_values == 0ULL);
  __VERIFIER_assert(unsigned_long_long_values[1] == unsigned_long_long_max);
  *unsigned_long_long_values = *unsigned_long_long_values - 1ULL;
  __VERIFIER_assert(*unsigned_long_long_values == unsigned_long_long_max);
  return 0;
}
