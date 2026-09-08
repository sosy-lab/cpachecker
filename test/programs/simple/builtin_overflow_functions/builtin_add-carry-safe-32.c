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

  // ILP32: 4294967295UL + 1UL = 4294967296; stored result = 0UL and carry-out = 1UL because the sum exceeds 4294967295UL.
  unsigned long int model_addcl_carry_out;
  (void)__builtin_addcl(4294967295UL, 1UL, 0UL, &model_addcl_carry_out);

  if (!(model_addcl_carry_out == 1UL))
    goto ERROR;

  // Unsigned int add-with-carry tests.

  unsigned int addc_zero_plus_zero_carry_zero_carry_out;
  unsigned int addc_zero_plus_zero_carry_zero_result;
  addc_zero_plus_zero_carry_zero_result = __builtin_addc(0U, 0U, 0U, &addc_zero_plus_zero_carry_zero_carry_out);

  // 0U + 0U + carry-in 0U = 0; result = 0U and carry-out = 0U because 0 <= 4294967295U.
  if (!(addc_zero_plus_zero_carry_zero_result == 0U))
    goto ERROR;

  if (!(addc_zero_plus_zero_carry_zero_carry_out == 0U))
    goto ERROR;


  unsigned int addc_unsigned_int_max_plus_zero_carry_zero_carry_out;
  unsigned int addc_unsigned_int_max_plus_zero_carry_zero_result;
  addc_unsigned_int_max_plus_zero_carry_zero_result = __builtin_addc(unsigned_int_max, 0U, 0U, &addc_unsigned_int_max_plus_zero_carry_zero_carry_out);

  // 4294967295U + 0U + carry-in 0U = 4294967295; result = 4294967295U and carry-out = 0U because 4294967295 <= 4294967295U.
  if (!(addc_unsigned_int_max_plus_zero_carry_zero_result == unsigned_int_max))
    goto ERROR;

  if (!(addc_unsigned_int_max_plus_zero_carry_zero_carry_out == 0U))
    goto ERROR;


  unsigned int addc_one_plus_two_carry_one_carry_out;
  unsigned int addc_one_plus_two_carry_one_result;
  addc_one_plus_two_carry_one_result = __builtin_addc(1U, 2U, 1U, &addc_one_plus_two_carry_one_carry_out);

  // 1U + 2U + carry-in 1U = 4; result = 4U and carry-out = 0U because 4 <= 4294967295U.
  if (!(addc_one_plus_two_carry_one_result == 4U))
    goto ERROR;

  if (!(addc_one_plus_two_carry_one_carry_out == 0U))
    goto ERROR;


  unsigned int addc_unsigned_int_max_plus_one_carry_zero_carry_out;
  unsigned int addc_unsigned_int_max_plus_one_carry_zero_result;
  addc_unsigned_int_max_plus_one_carry_zero_result = __builtin_addc(unsigned_int_max, 1U, 0U, &addc_unsigned_int_max_plus_one_carry_zero_carry_out);

  // 4294967295U + 1U + carry-in 0U = 4294967296; stored result = 0U and carry-out = 1U because 4294967296 > 4294967295U.
  if (!(addc_unsigned_int_max_plus_one_carry_zero_result == 0U))
    goto ERROR;

  if (!(addc_unsigned_int_max_plus_one_carry_zero_carry_out == 1U))
    goto ERROR;


  unsigned int addc_unsigned_int_max_plus_zero_carry_one_carry_out;
  unsigned int addc_unsigned_int_max_plus_zero_carry_one_result;
  addc_unsigned_int_max_plus_zero_carry_one_result = __builtin_addc(unsigned_int_max, 0U, 1U, &addc_unsigned_int_max_plus_zero_carry_one_carry_out);

  // 4294967295U + 0U + carry-in 1U = 4294967296; stored result = 0U and carry-out = 1U because 4294967296 > 4294967295U.
  if (!(addc_unsigned_int_max_plus_zero_carry_one_result == 0U))
    goto ERROR;

  if (!(addc_unsigned_int_max_plus_zero_carry_one_carry_out == 1U))
    goto ERROR;


  unsigned int addc_unsigned_int_max_plus_unsigned_int_max_carry_two_carry_out;
  unsigned int addc_unsigned_int_max_plus_unsigned_int_max_carry_two_result;
  addc_unsigned_int_max_plus_unsigned_int_max_carry_two_result = __builtin_addc(unsigned_int_max, unsigned_int_max, 2U, &addc_unsigned_int_max_plus_unsigned_int_max_carry_two_carry_out);

  // 4294967295U + 4294967295U + carry-in 2U = 8589934592; stored result = 0U and carry-out = 1U because 8589934592 > 4294967295U.
  if (!(addc_unsigned_int_max_plus_unsigned_int_max_carry_two_result == 0U))
    goto ERROR;

  if (!(addc_unsigned_int_max_plus_unsigned_int_max_carry_two_carry_out == 1U))
    goto ERROR;


  unsigned int addc_one_plus_zero_carry_unsigned_int_max_carry_out;
  unsigned int addc_one_plus_zero_carry_unsigned_int_max_result;
  addc_one_plus_zero_carry_unsigned_int_max_result = __builtin_addc(1U, 0U, unsigned_int_max, &addc_one_plus_zero_carry_unsigned_int_max_carry_out);

  // 1U + 0U + carry-in 4294967295U = 4294967296; stored result = 0U and carry-out = 1U because 4294967296 > 4294967295U.
  if (!(addc_one_plus_zero_carry_unsigned_int_max_result == 0U))
    goto ERROR;

  if (!(addc_one_plus_zero_carry_unsigned_int_max_carry_out == 1U))
    goto ERROR;


  // Unsigned long int add-with-carry tests.


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

  // 0UL + 0UL + carry-in 0UL = 0; result = 0UL and carry-out = 0UL because 0 <= 4294967295UL.
  if (!(addcl_zero_plus_zero_carry_zero_result == 0UL))
    goto ERROR;

  if (!(addcl_zero_plus_zero_carry_zero_carry_out == 0UL))
    goto ERROR;


  unsigned long int addcl_unsigned_long_max_plus_zero_carry_zero_carry_out;
  unsigned long int addcl_unsigned_long_max_plus_zero_carry_zero_result;
  addcl_unsigned_long_max_plus_zero_carry_zero_result = __builtin_addcl(addcl_unsigned_long_max, 0UL, 0UL, &addcl_unsigned_long_max_plus_zero_carry_zero_carry_out);

  // 4294967295UL + 0UL + carry-in 0UL = 4294967295; result = 4294967295UL and carry-out = 0UL because 4294967295 <= 4294967295UL.
  if (!(addcl_unsigned_long_max_plus_zero_carry_zero_result == addcl_unsigned_long_max))
    goto ERROR;

  if (!(addcl_unsigned_long_max_plus_zero_carry_zero_carry_out == 0UL))
    goto ERROR;


  unsigned long int addcl_one_plus_two_carry_one_carry_out;
  unsigned long int addcl_one_plus_two_carry_one_result;
  addcl_one_plus_two_carry_one_result = __builtin_addcl(1UL, 2UL, 1UL, &addcl_one_plus_two_carry_one_carry_out);

  // 1UL + 2UL + carry-in 1UL = 4; result = 4UL and carry-out = 0UL because 4 <= 4294967295UL.
  if (!(addcl_one_plus_two_carry_one_result == 4UL))
    goto ERROR;

  if (!(addcl_one_plus_two_carry_one_carry_out == 0UL))
    goto ERROR;


  unsigned long int addcl_unsigned_long_max_plus_one_carry_zero_carry_out;
  unsigned long int addcl_unsigned_long_max_plus_one_carry_zero_result;
  addcl_unsigned_long_max_plus_one_carry_zero_result = __builtin_addcl(addcl_unsigned_long_max, 1UL, 0UL, &addcl_unsigned_long_max_plus_one_carry_zero_carry_out);

  // 4294967295UL + 1UL + carry-in 0UL = 4294967296; stored result = 0UL and carry-out = 1UL because 4294967296 > 4294967295UL.
  if (!(addcl_unsigned_long_max_plus_one_carry_zero_result == 0UL))
    goto ERROR;

  if (!(addcl_unsigned_long_max_plus_one_carry_zero_carry_out == 1UL))
    goto ERROR;


  unsigned long int addcl_unsigned_long_max_plus_zero_carry_one_carry_out;
  unsigned long int addcl_unsigned_long_max_plus_zero_carry_one_result;
  addcl_unsigned_long_max_plus_zero_carry_one_result = __builtin_addcl(addcl_unsigned_long_max, 0UL, 1UL, &addcl_unsigned_long_max_plus_zero_carry_one_carry_out);

  // 4294967295UL + 0UL + carry-in 1UL = 4294967296; stored result = 0UL and carry-out = 1UL because 4294967296 > 4294967295UL.
  if (!(addcl_unsigned_long_max_plus_zero_carry_one_result == 0UL))
    goto ERROR;

  if (!(addcl_unsigned_long_max_plus_zero_carry_one_carry_out == 1UL))
    goto ERROR;


  unsigned long int addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out;
  unsigned long int addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_result;
  addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_result = __builtin_addcl(addcl_unsigned_long_max, addcl_unsigned_long_max, 2UL, &addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out);

  // 4294967295UL + 4294967295UL + carry-in 2UL = 8589934592; stored result = 0UL and carry-out = 1UL because 8589934592 > 4294967295UL.
  if (!(addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_result == 0UL))
    goto ERROR;

  if (!(addcl_unsigned_long_max_plus_unsigned_long_max_carry_two_carry_out == 1UL))
    goto ERROR;


  unsigned long int addcl_one_plus_zero_carry_unsigned_long_max_carry_out;
  unsigned long int addcl_one_plus_zero_carry_unsigned_long_max_result;
  addcl_one_plus_zero_carry_unsigned_long_max_result = __builtin_addcl(1UL, 0UL, addcl_unsigned_long_max, &addcl_one_plus_zero_carry_unsigned_long_max_carry_out);

  // 1UL + 0UL + carry-in 4294967295UL = 4294967296; stored result = 0UL and carry-out = 1UL because 4294967296 > 4294967295UL.
  if (!(addcl_one_plus_zero_carry_unsigned_long_max_result == 0UL))
    goto ERROR;

  if (!(addcl_one_plus_zero_carry_unsigned_long_max_carry_out == 1UL))
    goto ERROR;


  // Unsigned long long int add-with-carry tests.

  unsigned long long int addcll_zero_plus_zero_carry_zero_carry_out;
  unsigned long long int addcll_zero_plus_zero_carry_zero_result;
  addcll_zero_plus_zero_carry_zero_result = __builtin_addcll(0ULL, 0ULL, 0ULL, &addcll_zero_plus_zero_carry_zero_carry_out);

  // 0ULL + 0ULL + carry-in 0ULL = 0; result = 0ULL and carry-out = 0ULL because 0 <= 18446744073709551615ULL.
  if (!(addcll_zero_plus_zero_carry_zero_result == 0ULL))
    goto ERROR;

  if (!(addcll_zero_plus_zero_carry_zero_carry_out == 0ULL))
    goto ERROR;


  unsigned long long int addcll_unsigned_long_long_max_plus_zero_carry_zero_carry_out;
  unsigned long long int addcll_unsigned_long_long_max_plus_zero_carry_zero_result;
  addcll_unsigned_long_long_max_plus_zero_carry_zero_result = __builtin_addcll(unsigned_long_long_max, 0ULL, 0ULL, &addcll_unsigned_long_long_max_plus_zero_carry_zero_carry_out);

  // 18446744073709551615ULL + 0ULL + carry-in 0ULL = 18446744073709551615; result = 18446744073709551615ULL and carry-out = 0ULL because
  // 18446744073709551615 <= 18446744073709551615ULL.
  if (!(addcll_unsigned_long_long_max_plus_zero_carry_zero_result == unsigned_long_long_max))
    goto ERROR;

  if (!(addcll_unsigned_long_long_max_plus_zero_carry_zero_carry_out == 0ULL))
    goto ERROR;


  unsigned long long int addcll_one_plus_two_carry_one_carry_out;
  unsigned long long int addcll_one_plus_two_carry_one_result;
  addcll_one_plus_two_carry_one_result = __builtin_addcll(1ULL, 2ULL, 1ULL, &addcll_one_plus_two_carry_one_carry_out);

  // 1ULL + 2ULL + carry-in 1ULL = 4; result = 4ULL and carry-out = 0ULL because 4 <= 18446744073709551615ULL.
  if (!(addcll_one_plus_two_carry_one_result == 4ULL))
    goto ERROR;

  if (!(addcll_one_plus_two_carry_one_carry_out == 0ULL))
    goto ERROR;


  unsigned long long int addcll_unsigned_long_long_max_plus_one_carry_zero_carry_out;
  unsigned long long int addcll_unsigned_long_long_max_plus_one_carry_zero_result;
  addcll_unsigned_long_long_max_plus_one_carry_zero_result = __builtin_addcll(unsigned_long_long_max, 1ULL, 0ULL, &addcll_unsigned_long_long_max_plus_one_carry_zero_carry_out);

  // 18446744073709551615ULL + 1ULL + carry-in 0ULL = 18446744073709551616; stored result = 0ULL and carry-out = 1ULL because
  // 18446744073709551616 > 18446744073709551615ULL.
  if (!(addcll_unsigned_long_long_max_plus_one_carry_zero_result == 0ULL))
    goto ERROR;

  if (!(addcll_unsigned_long_long_max_plus_one_carry_zero_carry_out == 1ULL))
    goto ERROR;


  unsigned long long int addcll_unsigned_long_long_max_plus_zero_carry_one_carry_out;
  unsigned long long int addcll_unsigned_long_long_max_plus_zero_carry_one_result;
  addcll_unsigned_long_long_max_plus_zero_carry_one_result = __builtin_addcll(unsigned_long_long_max, 0ULL, 1ULL, &addcll_unsigned_long_long_max_plus_zero_carry_one_carry_out);

  // 18446744073709551615ULL + 0ULL + carry-in 1ULL = 18446744073709551616; stored result = 0ULL and carry-out = 1ULL because
  // 18446744073709551616 > 18446744073709551615ULL.
  if (!(addcll_unsigned_long_long_max_plus_zero_carry_one_result == 0ULL))
    goto ERROR;

  if (!(addcll_unsigned_long_long_max_plus_zero_carry_one_carry_out == 1ULL))
    goto ERROR;


  unsigned long long int addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_carry_out;
  unsigned long long int addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_result;
  addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_result = __builtin_addcll(unsigned_long_long_max, unsigned_long_long_max, 2ULL, &addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_carry_out);

  // 18446744073709551615ULL + 18446744073709551615ULL + carry-in 2ULL = 36893488147419103232; stored result = 0ULL and carry-out = 1ULL
  // because 36893488147419103232 > 18446744073709551615ULL.
  if (!(addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_result == 0ULL))
    goto ERROR;

  if (!(addcll_unsigned_long_long_max_plus_unsigned_long_long_max_carry_two_carry_out == 1ULL))
    goto ERROR;


  unsigned long long int addcll_one_plus_zero_carry_unsigned_long_long_max_carry_out;
  unsigned long long int addcll_one_plus_zero_carry_unsigned_long_long_max_result;
  addcll_one_plus_zero_carry_unsigned_long_long_max_result = __builtin_addcll(1ULL, 0ULL, unsigned_long_long_max, &addcll_one_plus_zero_carry_unsigned_long_long_max_carry_out);

  // 1ULL + 0ULL + carry-in 18446744073709551615ULL = 18446744073709551616; stored result = 0ULL and carry-out = 1ULL because
  // 18446744073709551616 > 18446744073709551615ULL.
  if (!(addcll_one_plus_zero_carry_unsigned_long_long_max_result == 0ULL))
    goto ERROR;

  if (!(addcll_one_plus_zero_carry_unsigned_long_long_max_carry_out == 1ULL))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
