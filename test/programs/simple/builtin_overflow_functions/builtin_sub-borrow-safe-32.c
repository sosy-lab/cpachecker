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
  const unsigned long int subcl_unsigned_long_max = ~0UL;


  // This program targets ILP32 and fails its expected verdict under LP64.

  // ILP32: 0UL - 1UL - borrow-in 4294967295UL = -4294967296; stored result = 0UL and borrow-out = 1UL.
  unsigned long int model_subcl_borrow_out;
  unsigned long int model_subcl_result;
  model_subcl_result = __builtin_subcl(0UL, 1UL, 4294967295UL, &model_subcl_borrow_out);

  if (!(model_subcl_result == 0UL && model_subcl_borrow_out == 1UL))
    goto ERROR;

  // Unsigned int subtract-with-borrow tests.

  unsigned int subc_zero_minus_zero_borrow_zero_borrow_out;
  unsigned int subc_zero_minus_zero_borrow_zero_result;
  subc_zero_minus_zero_borrow_zero_result = __builtin_subc(0U, 0U, 0U, &subc_zero_minus_zero_borrow_zero_borrow_out);

  // 0U - 0U - borrow-in 0U = 0; result = 0U and borrow-out = 0U because 0 >= 0.
  if (!(subc_zero_minus_zero_borrow_zero_result == 0U))
    goto ERROR;

  if (!(subc_zero_minus_zero_borrow_zero_borrow_out == 0U))
    goto ERROR;


  unsigned int subc_unsigned_int_max_minus_zero_borrow_zero_borrow_out;
  unsigned int subc_unsigned_int_max_minus_zero_borrow_zero_result;
  subc_unsigned_int_max_minus_zero_borrow_zero_result = __builtin_subc(unsigned_int_max, 0U, 0U, &subc_unsigned_int_max_minus_zero_borrow_zero_borrow_out);

  // 4294967295U - 0U - borrow-in 0U = 4294967295; result = 4294967295U and borrow-out = 0U because 4294967295 >= 0.
  if (!(subc_unsigned_int_max_minus_zero_borrow_zero_result == unsigned_int_max))
    goto ERROR;

  if (!(subc_unsigned_int_max_minus_zero_borrow_zero_borrow_out == 0U))
    goto ERROR;


  unsigned int subc_four_minus_two_borrow_one_borrow_out;
  unsigned int subc_four_minus_two_borrow_one_result;
  subc_four_minus_two_borrow_one_result = __builtin_subc(4U, 2U, 1U, &subc_four_minus_two_borrow_one_borrow_out);

  // 4U - 2U - borrow-in 1U = 1; result = 1U and borrow-out = 0U because 1 >= 0.
  if (!(subc_four_minus_two_borrow_one_result == 1U))
    goto ERROR;

  if (!(subc_four_minus_two_borrow_one_borrow_out == 0U))
    goto ERROR;


  unsigned int subc_zero_minus_one_borrow_zero_borrow_out;
  unsigned int subc_zero_minus_one_borrow_zero_result;
  subc_zero_minus_one_borrow_zero_result = __builtin_subc(0U, 1U, 0U, &subc_zero_minus_one_borrow_zero_borrow_out);

  // 0U - 1U - borrow-in 0U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  if (!(subc_zero_minus_one_borrow_zero_result == unsigned_int_max))
    goto ERROR;

  if (!(subc_zero_minus_one_borrow_zero_borrow_out == 1U))
    goto ERROR;


  unsigned int subc_zero_minus_zero_borrow_one_borrow_out;
  unsigned int subc_zero_minus_zero_borrow_one_result;
  subc_zero_minus_zero_borrow_one_result = __builtin_subc(0U, 0U, 1U, &subc_zero_minus_zero_borrow_one_borrow_out);

  // 0U - 0U - borrow-in 1U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  if (!(subc_zero_minus_zero_borrow_one_result == unsigned_int_max))
    goto ERROR;

  if (!(subc_zero_minus_zero_borrow_one_borrow_out == 1U))
    goto ERROR;


  unsigned int subc_zero_minus_two_borrow_unsigned_int_max_borrow_out;
  unsigned int subc_zero_minus_two_borrow_unsigned_int_max_result;
  subc_zero_minus_two_borrow_unsigned_int_max_result = __builtin_subc(0U, 2U, unsigned_int_max, &subc_zero_minus_two_borrow_unsigned_int_max_borrow_out);

  // 0U - 2U - borrow-in 4294967295U = -4294967297; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  if (!(subc_zero_minus_two_borrow_unsigned_int_max_result == unsigned_int_max))
    goto ERROR;

  if (!(subc_zero_minus_two_borrow_unsigned_int_max_borrow_out == 1U))
    goto ERROR;


  unsigned int subc_one_minus_zero_borrow_two_borrow_out;
  unsigned int subc_one_minus_zero_borrow_two_result;
  subc_one_minus_zero_borrow_two_result = __builtin_subc(1U, 0U, 2U, &subc_one_minus_zero_borrow_two_borrow_out);

  // 1U - 0U - borrow-in 2U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  if (!(subc_one_minus_zero_borrow_two_result == unsigned_int_max))
    goto ERROR;

  if (!(subc_one_minus_zero_borrow_two_borrow_out == 1U))
    goto ERROR;


  // Unsigned long int subtract-with-borrow tests.


  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  if (!((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)subcl_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)subcl_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max)))
    goto ERROR;


  unsigned long int subcl_zero_minus_zero_borrow_zero_borrow_out;
  unsigned long int subcl_zero_minus_zero_borrow_zero_result;
  subcl_zero_minus_zero_borrow_zero_result = __builtin_subcl(0UL, 0UL, 0UL, &subcl_zero_minus_zero_borrow_zero_borrow_out);

  // 0UL - 0UL - borrow-in 0UL = 0; result = 0UL and borrow-out = 0UL because 0 >= 0.
  if (!(subcl_zero_minus_zero_borrow_zero_result == 0UL))
    goto ERROR;

  if (!(subcl_zero_minus_zero_borrow_zero_borrow_out == 0UL))
    goto ERROR;


  unsigned long int subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out;
  unsigned long int subcl_unsigned_long_max_minus_zero_borrow_zero_result;
  subcl_unsigned_long_max_minus_zero_borrow_zero_result = __builtin_subcl(subcl_unsigned_long_max, 0UL, 0UL, &subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out);

  // 4294967295UL - 0UL - borrow-in 0UL = 4294967295; result = 4294967295UL and borrow-out = 0UL because 4294967295 >= 0.
  if (!(subcl_unsigned_long_max_minus_zero_borrow_zero_result == subcl_unsigned_long_max))
    goto ERROR;

  if (!(subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out == 0UL))
    goto ERROR;


  unsigned long int subcl_four_minus_two_borrow_one_borrow_out;
  unsigned long int subcl_four_minus_two_borrow_one_result;
  subcl_four_minus_two_borrow_one_result = __builtin_subcl(4UL, 2UL, 1UL, &subcl_four_minus_two_borrow_one_borrow_out);

  // 4UL - 2UL - borrow-in 1UL = 1; result = 1UL and borrow-out = 0UL because 1 >= 0.
  if (!(subcl_four_minus_two_borrow_one_result == 1UL))
    goto ERROR;

  if (!(subcl_four_minus_two_borrow_one_borrow_out == 0UL))
    goto ERROR;


  unsigned long int subcl_zero_minus_one_borrow_zero_borrow_out;
  unsigned long int subcl_zero_minus_one_borrow_zero_result;
  subcl_zero_minus_one_borrow_zero_result = __builtin_subcl(0UL, 1UL, 0UL, &subcl_zero_minus_one_borrow_zero_borrow_out);

  // 0UL - 1UL - borrow-in 0UL = -1; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage underflows.
  if (!(subcl_zero_minus_one_borrow_zero_result == subcl_unsigned_long_max))
    goto ERROR;

  if (!(subcl_zero_minus_one_borrow_zero_borrow_out == 1UL))
    goto ERROR;


  unsigned long int subcl_zero_minus_zero_borrow_one_borrow_out;
  unsigned long int subcl_zero_minus_zero_borrow_one_result;
  subcl_zero_minus_zero_borrow_one_result = __builtin_subcl(0UL, 0UL, 1UL, &subcl_zero_minus_zero_borrow_one_borrow_out);

  // 0UL - 0UL - borrow-in 1UL = -1; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage underflows.
  if (!(subcl_zero_minus_zero_borrow_one_result == subcl_unsigned_long_max))
    goto ERROR;

  if (!(subcl_zero_minus_zero_borrow_one_borrow_out == 1UL))
    goto ERROR;


  unsigned long int subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out;
  unsigned long int subcl_zero_minus_two_borrow_unsigned_long_max_result;
  subcl_zero_minus_two_borrow_unsigned_long_max_result = __builtin_subcl(0UL, 2UL, subcl_unsigned_long_max, &subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out);

  // 0UL - 2UL - borrow-in 4294967295UL = -4294967297; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage
  // underflows.
  if (!(subcl_zero_minus_two_borrow_unsigned_long_max_result == subcl_unsigned_long_max))
    goto ERROR;

  if (!(subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out == 1UL))
    goto ERROR;


  unsigned long int subcl_one_minus_zero_borrow_two_borrow_out;
  unsigned long int subcl_one_minus_zero_borrow_two_result;
  subcl_one_minus_zero_borrow_two_result = __builtin_subcl(1UL, 0UL, 2UL, &subcl_one_minus_zero_borrow_two_borrow_out);

  // 1UL - 0UL - borrow-in 2UL = -1; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage underflows.
  if (!(subcl_one_minus_zero_borrow_two_result == subcl_unsigned_long_max))
    goto ERROR;

  if (!(subcl_one_minus_zero_borrow_two_borrow_out == 1UL))
    goto ERROR;


  // Unsigned long long int subtract-with-borrow tests.

  unsigned long long int subcll_zero_minus_zero_borrow_zero_borrow_out;
  unsigned long long int subcll_zero_minus_zero_borrow_zero_result;
  subcll_zero_minus_zero_borrow_zero_result = __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_zero_minus_zero_borrow_zero_borrow_out);

  // 0ULL - 0ULL - borrow-in 0ULL = 0; result = 0ULL and borrow-out = 0ULL because 0 >= 0.
  if (!(subcll_zero_minus_zero_borrow_zero_result == 0ULL))
    goto ERROR;

  if (!(subcll_zero_minus_zero_borrow_zero_borrow_out == 0ULL))
    goto ERROR;


  unsigned long long int subcll_unsigned_long_long_max_minus_zero_borrow_zero_borrow_out;
  unsigned long long int subcll_unsigned_long_long_max_minus_zero_borrow_zero_result;
  subcll_unsigned_long_long_max_minus_zero_borrow_zero_result = __builtin_subcll(unsigned_long_long_max, 0ULL, 0ULL, &subcll_unsigned_long_long_max_minus_zero_borrow_zero_borrow_out);

  // 18446744073709551615ULL - 0ULL - borrow-in 0ULL = 18446744073709551615; result = 18446744073709551615ULL and borrow-out = 0ULL because
  // 18446744073709551615 >= 0.
  if (!(subcll_unsigned_long_long_max_minus_zero_borrow_zero_result == unsigned_long_long_max))
    goto ERROR;

  if (!(subcll_unsigned_long_long_max_minus_zero_borrow_zero_borrow_out == 0ULL))
    goto ERROR;


  unsigned long long int subcll_four_minus_two_borrow_one_borrow_out;
  unsigned long long int subcll_four_minus_two_borrow_one_result;
  subcll_four_minus_two_borrow_one_result = __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_four_minus_two_borrow_one_borrow_out);

  // 4ULL - 2ULL - borrow-in 1ULL = 1; result = 1ULL and borrow-out = 0ULL because 1 >= 0.
  if (!(subcll_four_minus_two_borrow_one_result == 1ULL))
    goto ERROR;

  if (!(subcll_four_minus_two_borrow_one_borrow_out == 0ULL))
    goto ERROR;


  unsigned long long int subcll_zero_minus_one_borrow_zero_borrow_out;
  unsigned long long int subcll_zero_minus_one_borrow_zero_result;
  subcll_zero_minus_one_borrow_zero_result = __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_zero_minus_one_borrow_zero_borrow_out);

  // 0ULL - 1ULL - borrow-in 0ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  if (!(subcll_zero_minus_one_borrow_zero_result == unsigned_long_long_max))
    goto ERROR;

  if (!(subcll_zero_minus_one_borrow_zero_borrow_out == 1ULL))
    goto ERROR;


  unsigned long long int subcll_zero_minus_zero_borrow_one_borrow_out;
  unsigned long long int subcll_zero_minus_zero_borrow_one_result;
  subcll_zero_minus_zero_borrow_one_result = __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_zero_minus_zero_borrow_one_borrow_out);

  // 0ULL - 0ULL - borrow-in 1ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  if (!(subcll_zero_minus_zero_borrow_one_result == unsigned_long_long_max))
    goto ERROR;

  if (!(subcll_zero_minus_zero_borrow_one_borrow_out == 1ULL))
    goto ERROR;


  unsigned long long int subcll_zero_minus_two_borrow_unsigned_long_long_max_borrow_out;
  unsigned long long int subcll_zero_minus_two_borrow_unsigned_long_long_max_result;
  subcll_zero_minus_two_borrow_unsigned_long_long_max_result = __builtin_subcll(0ULL, 2ULL, unsigned_long_long_max, &subcll_zero_minus_two_borrow_unsigned_long_long_max_borrow_out);

  // 0ULL - 2ULL - borrow-in 18446744073709551615ULL = -18446744073709551617; stored result = 18446744073709551615ULL and borrow-out = 1ULL
  // because a subtraction stage underflows.
  if (!(subcll_zero_minus_two_borrow_unsigned_long_long_max_result == unsigned_long_long_max))
    goto ERROR;

  if (!(subcll_zero_minus_two_borrow_unsigned_long_long_max_borrow_out == 1ULL))
    goto ERROR;


  unsigned long long int subcll_one_minus_zero_borrow_two_borrow_out;
  unsigned long long int subcll_one_minus_zero_borrow_two_result;
  subcll_one_minus_zero_borrow_two_result = __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_one_minus_zero_borrow_two_borrow_out);

  // 1ULL - 0ULL - borrow-in 2ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  if (!(subcll_one_minus_zero_borrow_two_result == unsigned_long_long_max))
    goto ERROR;

  if (!(subcll_one_minus_zero_borrow_two_borrow_out == 1ULL))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
