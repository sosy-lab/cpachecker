// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


int main(void) {

  // Constant test values are initialized directly and never modified.
  const unsigned int unsigned_int_max = 4294967295U;
  const unsigned long long int unsigned_long_long_max = 18446744073709551615ULL;
  const unsigned long int addcl_unsigned_long_max = ~0UL;


  // This program targets ILP32 and fails its expected verdict under LP64.

  // ILP32: 4294967295UL + 1UL carries out of unsigned long, so carry-out is 1UL.
  unsigned long int model_addcl_carry_out;
  (void)__builtin_addcl(4294967295UL, 1UL, 0UL, &model_addcl_carry_out);

  if (!(model_addcl_carry_out == 1UL))
    goto ERROR;

  // Tests for __builtin_addc.

  unsigned int addc_zero_plus_zero_carry_zero_carry_out;
  unsigned int addc_zero_plus_zero_carry_zero_result;
  addc_zero_plus_zero_carry_zero_result = __builtin_addc(0U, 0U, 0U, &addc_zero_plus_zero_carry_zero_carry_out);

  // __builtin_addc(0U, 0U, 0U, &addc_zero_plus_zero_carry_zero_carry_out) returns 0U.
  if (!(addc_zero_plus_zero_carry_zero_result == 0U))
    goto ERROR;

  // __builtin_addc(0U, 0U, 0U, &addc_zero_plus_zero_carry_zero_carry_out) stores 0U.
  if (!(addc_zero_plus_zero_carry_zero_carry_out == 0U))
    goto ERROR;


  unsigned int addc_unsigned_int_max_plus_zero_carry_zero_carry_out;
  unsigned int addc_unsigned_int_max_plus_zero_carry_zero_result;
  addc_unsigned_int_max_plus_zero_carry_zero_result = __builtin_addc(unsigned_int_max, 0U, 0U, &addc_unsigned_int_max_plus_zero_carry_zero_carry_out);

  // __builtin_addc(4294967295U, 0U, 0U, &addc_unsigned_int_max_plus_zero_carry_zero_carry_out) returns 4294967295U.
  if (!(addc_unsigned_int_max_plus_zero_carry_zero_result == unsigned_int_max))
    goto ERROR;

  // __builtin_addc(4294967295U, 0U, 0U, &addc_unsigned_int_max_plus_zero_carry_zero_carry_out) stores 0U.
  if (!(addc_unsigned_int_max_plus_zero_carry_zero_carry_out == 0U))
    goto ERROR;


  unsigned int addc_one_plus_two_carry_one_carry_out;
  unsigned int addc_one_plus_two_carry_one_result;
  addc_one_plus_two_carry_one_result = __builtin_addc(1U, 2U, 1U, &addc_one_plus_two_carry_one_carry_out);

  // __builtin_addc(1U, 2U, 1U, &addc_one_plus_two_carry_one_carry_out) returns 4U.
  if (!(addc_one_plus_two_carry_one_result == 4U))
    goto ERROR;

  // __builtin_addc(1U, 2U, 1U, &addc_one_plus_two_carry_one_carry_out) stores 0U.
  if (!(addc_one_plus_two_carry_one_carry_out == 0U))
    goto ERROR;


  unsigned int addc_unsigned_int_max_plus_one_carry_zero_carry_out;
  unsigned int addc_unsigned_int_max_plus_one_carry_zero_result;
  addc_unsigned_int_max_plus_one_carry_zero_result = __builtin_addc(unsigned_int_max, 1U, 0U, &addc_unsigned_int_max_plus_one_carry_zero_carry_out);

  // __builtin_addc(4294967295U, 1U, 0U, &addc_unsigned_int_max_plus_one_carry_zero_carry_out) returns 0U.
  if (!(addc_unsigned_int_max_plus_one_carry_zero_result == 0U))
    goto ERROR;

  // __builtin_addc(4294967295U, 1U, 0U, &addc_unsigned_int_max_plus_one_carry_zero_carry_out) stores 1U.
  if (!(addc_unsigned_int_max_plus_one_carry_zero_carry_out == 1U))
    goto ERROR;


  unsigned int addc_unsigned_int_max_plus_zero_carry_one_carry_out;
  unsigned int addc_unsigned_int_max_plus_zero_carry_one_result;
  addc_unsigned_int_max_plus_zero_carry_one_result = __builtin_addc(unsigned_int_max, 0U, 1U, &addc_unsigned_int_max_plus_zero_carry_one_carry_out);

  // __builtin_addc(4294967295U, 0U, 1U, &addc_unsigned_int_max_plus_zero_carry_one_carry_out) returns 0U.
  if (!(addc_unsigned_int_max_plus_zero_carry_one_result == 0U))
    goto ERROR;

  // __builtin_addc(4294967295U, 0U, 1U, &addc_unsigned_int_max_plus_zero_carry_one_carry_out) stores 1U.
  if (!(addc_unsigned_int_max_plus_zero_carry_one_carry_out == 1U))
    goto ERROR;


  unsigned int addc_unsigned_int_max_plus_unsigned_int_max_carry_two_carry_out;
  unsigned int addc_unsigned_int_max_plus_unsigned_int_max_carry_two_result;
  addc_unsigned_int_max_plus_unsigned_int_max_carry_two_result = __builtin_addc(unsigned_int_max, unsigned_int_max, 2U, &addc_unsigned_int_max_plus_unsigned_int_max_carry_two_carry_out);

  // __builtin_addc(4294967295U, 4294967295U, 2U, &addc_unsigned_int_max_plus_unsigned_int_max_carry_two_carry_out) returns 0U.
  if (!(addc_unsigned_int_max_plus_unsigned_int_max_carry_two_result == 0U))
    goto ERROR;

  // __builtin_addc(4294967295U, 4294967295U, 2U, &addc_unsigned_int_max_plus_unsigned_int_max_carry_two_carry_out) stores 1U.
  if (!(addc_unsigned_int_max_plus_unsigned_int_max_carry_two_carry_out == 1U))
    goto ERROR;


  unsigned int addc_one_plus_zero_carry_unsigned_int_max_carry_out;
  unsigned int addc_one_plus_zero_carry_unsigned_int_max_result;
  addc_one_plus_zero_carry_unsigned_int_max_result = __builtin_addc(1U, 0U, unsigned_int_max, &addc_one_plus_zero_carry_unsigned_int_max_carry_out);

  // __builtin_addc(1U, 0U, 4294967295U, &addc_one_plus_zero_carry_unsigned_int_max_carry_out) returns 0U.
  if (!(addc_one_plus_zero_carry_unsigned_int_max_result == 0U))
    goto ERROR;

  // __builtin_addc(1U, 0U, 4294967295U, &addc_one_plus_zero_carry_unsigned_int_max_carry_out) stores 1U.
  if (!(addc_one_plus_zero_carry_unsigned_int_max_carry_out == 1U))
    goto ERROR;


  // Tests for __builtin_addcl.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)addcl_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)addcl_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  unsigned long int addcl_zero_plus_zero_carry_zero_carry_out;
  unsigned long int addcl_zero_plus_zero_carry_zero_result;
  addcl_zero_plus_zero_carry_zero_result = __builtin_addcl(0UL, 0UL, 0UL, &addcl_zero_plus_zero_carry_zero_carry_out);

  // __builtin_addcl(0UL, 0UL, 0UL, &addcl_zero_plus_zero_carry_zero_carry_out) returns 0UL.
  if (!(addcl_zero_plus_zero_carry_zero_result == 0UL))
    goto ERROR;

  // __builtin_addcl(0UL, 0UL, 0UL, &addcl_zero_plus_zero_carry_zero_carry_out) stores 0UL.
  if (!(addcl_zero_plus_zero_carry_zero_carry_out == 0UL))
    goto ERROR;


  unsigned long int addcl_unsigned_long_max_plus_zero_carry_zero_carry_out;
  unsigned long int addcl_unsigned_long_max_plus_zero_carry_zero_result;
  addcl_unsigned_long_max_plus_zero_carry_zero_result = __builtin_addcl(addcl_unsigned_long_max, 0UL, 0UL, &addcl_unsigned_long_max_plus_zero_carry_zero_carry_out);

  // ILP32: __builtin_addcl(4294967295UL, 0UL, 0UL, &addcl_unsigned_long_max_plus_zero_carry_zero_carry_out) returns 4294967295UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 0UL, 0UL, &addcl_unsigned_long_max_plus_zero_carry_zero_carry_out) returns
  // 18446744073709551615UL.
  if (!(addcl_unsigned_long_max_plus_zero_carry_zero_result == addcl_unsigned_long_max))
    goto ERROR;

  // ILP32: __builtin_addcl(4294967295UL, 0UL, 0UL, &addcl_unsigned_long_max_plus_zero_carry_zero_carry_out) stores 0UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 0UL, 0UL, &addcl_unsigned_long_max_plus_zero_carry_zero_carry_out) stores 0UL.
  if (!(addcl_unsigned_long_max_plus_zero_carry_zero_carry_out == 0UL))
    goto ERROR;


  unsigned long int addcl_one_plus_two_carry_one_carry_out;
  unsigned long int addcl_one_plus_two_carry_one_result;
  addcl_one_plus_two_carry_one_result = __builtin_addcl(1UL, 2UL, 1UL, &addcl_one_plus_two_carry_one_carry_out);

  // __builtin_addcl(1UL, 2UL, 1UL, &addcl_one_plus_two_carry_one_carry_out) returns 4UL.
  if (!(addcl_one_plus_two_carry_one_result == 4UL))
    goto ERROR;

  // __builtin_addcl(1UL, 2UL, 1UL, &addcl_one_plus_two_carry_one_carry_out) stores 0UL.
  if (!(addcl_one_plus_two_carry_one_carry_out == 0UL))
    goto ERROR;


  unsigned long int addcl_unsigned_long_max_plus_one_carry_zero_carry_out;
  unsigned long int addcl_unsigned_long_max_plus_one_carry_zero_result;
  addcl_unsigned_long_max_plus_one_carry_zero_result = __builtin_addcl(addcl_unsigned_long_max, 1UL, 0UL, &addcl_unsigned_long_max_plus_one_carry_zero_carry_out);

  // ILP32: __builtin_addcl(4294967295UL, 1UL, 0UL, &addcl_unsigned_long_max_plus_one_carry_zero_carry_out) returns 0UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 1UL, 0UL, &addcl_unsigned_long_max_plus_one_carry_zero_carry_out) returns 0UL.
  if (!(addcl_unsigned_long_max_plus_one_carry_zero_result == 0UL))
    goto ERROR;

  // ILP32: __builtin_addcl(4294967295UL, 1UL, 0UL, &addcl_unsigned_long_max_plus_one_carry_zero_carry_out) stores 1UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 1UL, 0UL, &addcl_unsigned_long_max_plus_one_carry_zero_carry_out) stores 1UL.
  if (!(addcl_unsigned_long_max_plus_one_carry_zero_carry_out == 1UL))
    goto ERROR;


  unsigned long int addcl_unsigned_long_max_plus_zero_carry_one_carry_out;
  unsigned long int addcl_unsigned_long_max_plus_zero_carry_one_result;
  addcl_unsigned_long_max_plus_zero_carry_one_result = __builtin_addcl(addcl_unsigned_long_max, 0UL, 1UL, &addcl_unsigned_long_max_plus_zero_carry_one_carry_out);

  // ILP32: __builtin_addcl(4294967295UL, 0UL, 1UL, &addcl_unsigned_long_max_plus_zero_carry_one_carry_out) returns 0UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 0UL, 1UL, &addcl_unsigned_long_max_plus_zero_carry_one_carry_out) returns 0UL.
  if (!(addcl_unsigned_long_max_plus_zero_carry_one_result == 0UL))
    goto ERROR;

  // ILP32: __builtin_addcl(4294967295UL, 0UL, 1UL, &addcl_unsigned_long_max_plus_zero_carry_one_carry_out) stores 1UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 0UL, 1UL, &addcl_unsigned_long_max_plus_zero_carry_one_carry_out) stores 1UL.
  if (!(addcl_unsigned_long_max_plus_zero_carry_one_carry_out == 1UL))
    goto ERROR;


  unsigned long int addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out;
  unsigned long int addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_result;
  addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_result = __builtin_addcl(addcl_unsigned_long_max, addcl_unsigned_long_max, 2UL, &addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out);

  // ILP32: __builtin_addcl(4294967295UL, 4294967295UL, 2UL, &addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out) returns
  // 0UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 18446744073709551615UL, 2UL,
  // &addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out) returns 0UL.
  if (!(addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_result == 0UL))
    goto ERROR;

  // ILP32: __builtin_addcl(4294967295UL, 4294967295UL, 2UL, &addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out) stores
  // 1UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 18446744073709551615UL, 2UL,
  // &addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out) stores 1UL.
  if (!(addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out == 1UL))
    goto ERROR;


  unsigned long int addcl_one_plus_zero_carry_unsigned_long_max_carry_out;
  unsigned long int addcl_one_plus_zero_carry_unsigned_long_max_result;
  addcl_one_plus_zero_carry_unsigned_long_max_result = __builtin_addcl(1UL, 0UL, addcl_unsigned_long_max, &addcl_one_plus_zero_carry_unsigned_long_max_carry_out);

  // ILP32: __builtin_addcl(1UL, 0UL, 4294967295UL, &addcl_one_plus_zero_carry_unsigned_long_max_carry_out) returns 0UL.
  // LP64: __builtin_addcl(1UL, 0UL, 18446744073709551615UL, &addcl_one_plus_zero_carry_unsigned_long_max_carry_out) returns 0UL.
  if (!(addcl_one_plus_zero_carry_unsigned_long_max_result == 0UL))
    goto ERROR;

  // ILP32: __builtin_addcl(1UL, 0UL, 4294967295UL, &addcl_one_plus_zero_carry_unsigned_long_max_carry_out) stores 1UL.
  // LP64: __builtin_addcl(1UL, 0UL, 18446744073709551615UL, &addcl_one_plus_zero_carry_unsigned_long_max_carry_out) stores 1UL.
  if (!(addcl_one_plus_zero_carry_unsigned_long_max_carry_out == 1UL))
    goto ERROR;


  // Tests for __builtin_addcll.

  unsigned long long int addcll_zero_plus_zero_carry_zero_carry_out;
  unsigned long long int addcll_zero_plus_zero_carry_zero_result;
  addcll_zero_plus_zero_carry_zero_result = __builtin_addcll(0ULL, 0ULL, 0ULL, &addcll_zero_plus_zero_carry_zero_carry_out);

  // __builtin_addcll(0ULL, 0ULL, 0ULL, &addcll_zero_plus_zero_carry_zero_carry_out) returns 0ULL.
  if (!(addcll_zero_plus_zero_carry_zero_result == 0ULL))
    goto ERROR;

  // __builtin_addcll(0ULL, 0ULL, 0ULL, &addcll_zero_plus_zero_carry_zero_carry_out) stores 0ULL.
  if (!(addcll_zero_plus_zero_carry_zero_carry_out == 0ULL))
    goto ERROR;


  unsigned long long int addcll_unsigned_long_long_max_plus_zero_carry_zero_carry_out;
  unsigned long long int addcll_unsigned_long_long_max_plus_zero_carry_zero_result;
  addcll_unsigned_long_long_max_plus_zero_carry_zero_result = __builtin_addcll(unsigned_long_long_max, 0ULL, 0ULL, &addcll_unsigned_long_long_max_plus_zero_carry_zero_carry_out);

  // __builtin_addcll(18446744073709551615ULL, 0ULL, 0ULL, &addcll_unsigned_long_long_max_plus_zero_carry_zero_carry_out) returns
  // 18446744073709551615ULL.
  if (!(addcll_unsigned_long_long_max_plus_zero_carry_zero_result == unsigned_long_long_max))
    goto ERROR;

  // __builtin_addcll(18446744073709551615ULL, 0ULL, 0ULL, &addcll_unsigned_long_long_max_plus_zero_carry_zero_carry_out) stores 0ULL.
  if (!(addcll_unsigned_long_long_max_plus_zero_carry_zero_carry_out == 0ULL))
    goto ERROR;


  unsigned long long int addcll_one_plus_two_carry_one_carry_out;
  unsigned long long int addcll_one_plus_two_carry_one_result;
  addcll_one_plus_two_carry_one_result = __builtin_addcll(1ULL, 2ULL, 1ULL, &addcll_one_plus_two_carry_one_carry_out);

  // __builtin_addcll(1ULL, 2ULL, 1ULL, &addcll_one_plus_two_carry_one_carry_out) returns 4ULL.
  if (!(addcll_one_plus_two_carry_one_result == 4ULL))
    goto ERROR;

  // __builtin_addcll(1ULL, 2ULL, 1ULL, &addcll_one_plus_two_carry_one_carry_out) stores 0ULL.
  if (!(addcll_one_plus_two_carry_one_carry_out == 0ULL))
    goto ERROR;


  unsigned long long int addcll_unsigned_long_long_max_plus_one_carry_zero_carry_out;
  unsigned long long int addcll_unsigned_long_long_max_plus_one_carry_zero_result;
  addcll_unsigned_long_long_max_plus_one_carry_zero_result = __builtin_addcll(unsigned_long_long_max, 1ULL, 0ULL, &addcll_unsigned_long_long_max_plus_one_carry_zero_carry_out);

  // __builtin_addcll(18446744073709551615ULL, 1ULL, 0ULL, &addcll_unsigned_long_long_max_plus_one_carry_zero_carry_out) returns 0ULL.
  if (!(addcll_unsigned_long_long_max_plus_one_carry_zero_result == 0ULL))
    goto ERROR;

  // __builtin_addcll(18446744073709551615ULL, 1ULL, 0ULL, &addcll_unsigned_long_long_max_plus_one_carry_zero_carry_out) stores 1ULL.
  if (!(addcll_unsigned_long_long_max_plus_one_carry_zero_carry_out == 1ULL))
    goto ERROR;


  unsigned long long int addcll_unsigned_long_long_max_plus_zero_carry_one_carry_out;
  unsigned long long int addcll_unsigned_long_long_max_plus_zero_carry_one_result;
  addcll_unsigned_long_long_max_plus_zero_carry_one_result = __builtin_addcll(unsigned_long_long_max, 0ULL, 1ULL, &addcll_unsigned_long_long_max_plus_zero_carry_one_carry_out);

  // __builtin_addcll(18446744073709551615ULL, 0ULL, 1ULL, &addcll_unsigned_long_long_max_plus_zero_carry_one_carry_out) returns 0ULL.
  if (!(addcll_unsigned_long_long_max_plus_zero_carry_one_result == 0ULL))
    goto ERROR;

  // __builtin_addcll(18446744073709551615ULL, 0ULL, 1ULL, &addcll_unsigned_long_long_max_plus_zero_carry_one_carry_out) stores 1ULL.
  if (!(addcll_unsigned_long_long_max_plus_zero_carry_one_carry_out == 1ULL))
    goto ERROR;


  unsigned long long int addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_carry_out;
  unsigned long long int addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_result;
  addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_result = __builtin_addcll(unsigned_long_long_max, unsigned_long_long_max, 2ULL, &addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_carry_out);

  // __builtin_addcll(18446744073709551615ULL, 18446744073709551615ULL, 2ULL,
  // &addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_carry_out) returns 0ULL.
  if (!(addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_result == 0ULL))
    goto ERROR;

  // __builtin_addcll(18446744073709551615ULL, 18446744073709551615ULL, 2ULL,
  // &addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_carry_out) stores 1ULL.
  if (!(addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_carry_out == 1ULL))
    goto ERROR;


  unsigned long long int addcll_one_plus_zero_carry_unsigned_long_long_max_carry_out;
  unsigned long long int addcll_one_plus_zero_carry_unsigned_long_long_max_result;
  addcll_one_plus_zero_carry_unsigned_long_long_max_result = __builtin_addcll(1ULL, 0ULL, unsigned_long_long_max, &addcll_one_plus_zero_carry_unsigned_long_long_max_carry_out);

  // __builtin_addcll(1ULL, 0ULL, 18446744073709551615ULL, &addcll_one_plus_zero_carry_unsigned_long_long_max_carry_out) returns 0ULL.
  if (!(addcll_one_plus_zero_carry_unsigned_long_long_max_result == 0ULL))
    goto ERROR;

  // __builtin_addcll(1ULL, 0ULL, 18446744073709551615ULL, &addcll_one_plus_zero_carry_unsigned_long_long_max_carry_out) stores 1ULL.
  if (!(addcll_one_plus_zero_carry_unsigned_long_long_max_carry_out == 1ULL))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
