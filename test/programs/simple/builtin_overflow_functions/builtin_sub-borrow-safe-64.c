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


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: 0UL - 1UL - 4294967295UL stores 18446744069414584320UL and borrows.
  unsigned long int model_subcl_borrow_out;
  unsigned long int model_subcl_result;
  model_subcl_result = __builtin_subcl(0UL, 1UL, 4294967295UL, &model_subcl_borrow_out);

  if (!(model_subcl_result != 0UL && model_subcl_borrow_out == 1UL))
    goto ERROR;

  // Tests for __builtin_subc.

  unsigned int subc_zero_minus_zero_borrow_zero_borrow_out;
  unsigned int subc_zero_minus_zero_borrow_zero_result;
  subc_zero_minus_zero_borrow_zero_result = __builtin_subc(0U, 0U, 0U, &subc_zero_minus_zero_borrow_zero_borrow_out);

  // __builtin_subc(0U, 0U, 0U, &subc_zero_minus_zero_borrow_zero_borrow_out) returns 0U.
  if (!(subc_zero_minus_zero_borrow_zero_result == 0U))
    goto ERROR;

  // __builtin_subc(0U, 0U, 0U, &subc_zero_minus_zero_borrow_zero_borrow_out) stores 0U.
  if (!(subc_zero_minus_zero_borrow_zero_borrow_out == 0U))
    goto ERROR;


  unsigned int subc_unsigned_int_max_minus_zero_borrow_zero_borrow_out;
  unsigned int subc_unsigned_int_max_minus_zero_borrow_zero_result;
  subc_unsigned_int_max_minus_zero_borrow_zero_result = __builtin_subc(unsigned_int_max, 0U, 0U, &subc_unsigned_int_max_minus_zero_borrow_zero_borrow_out);

  // __builtin_subc(4294967295U, 0U, 0U, &subc_unsigned_int_max_minus_zero_borrow_zero_borrow_out) returns 4294967295U.
  if (!(subc_unsigned_int_max_minus_zero_borrow_zero_result == unsigned_int_max))
    goto ERROR;

  // __builtin_subc(4294967295U, 0U, 0U, &subc_unsigned_int_max_minus_zero_borrow_zero_borrow_out) stores 0U.
  if (!(subc_unsigned_int_max_minus_zero_borrow_zero_borrow_out == 0U))
    goto ERROR;


  unsigned int subc_four_minus_two_borrow_one_borrow_out;
  unsigned int subc_four_minus_two_borrow_one_result;
  subc_four_minus_two_borrow_one_result = __builtin_subc(4U, 2U, 1U, &subc_four_minus_two_borrow_one_borrow_out);

  // __builtin_subc(4U, 2U, 1U, &subc_four_minus_two_borrow_one_borrow_out) returns 1U.
  if (!(subc_four_minus_two_borrow_one_result == 1U))
    goto ERROR;

  // __builtin_subc(4U, 2U, 1U, &subc_four_minus_two_borrow_one_borrow_out) stores 0U.
  if (!(subc_four_minus_two_borrow_one_borrow_out == 0U))
    goto ERROR;


  unsigned int subc_zero_minus_one_borrow_zero_borrow_out;
  unsigned int subc_zero_minus_one_borrow_zero_result;
  subc_zero_minus_one_borrow_zero_result = __builtin_subc(0U, 1U, 0U, &subc_zero_minus_one_borrow_zero_borrow_out);

  // __builtin_subc(0U, 1U, 0U, &subc_zero_minus_one_borrow_zero_borrow_out) returns 4294967295U.
  if (!(subc_zero_minus_one_borrow_zero_result == unsigned_int_max))
    goto ERROR;

  // __builtin_subc(0U, 1U, 0U, &subc_zero_minus_one_borrow_zero_borrow_out) stores 1U.
  if (!(subc_zero_minus_one_borrow_zero_borrow_out == 1U))
    goto ERROR;


  unsigned int subc_zero_minus_zero_borrow_one_borrow_out;
  unsigned int subc_zero_minus_zero_borrow_one_result;
  subc_zero_minus_zero_borrow_one_result = __builtin_subc(0U, 0U, 1U, &subc_zero_minus_zero_borrow_one_borrow_out);

  // __builtin_subc(0U, 0U, 1U, &subc_zero_minus_zero_borrow_one_borrow_out) returns 4294967295U.
  if (!(subc_zero_minus_zero_borrow_one_result == unsigned_int_max))
    goto ERROR;

  // __builtin_subc(0U, 0U, 1U, &subc_zero_minus_zero_borrow_one_borrow_out) stores 1U.
  if (!(subc_zero_minus_zero_borrow_one_borrow_out == 1U))
    goto ERROR;


  unsigned int subc_zero_minus_two_borrow_unsigned_int_max_borrow_out;
  unsigned int subc_zero_minus_two_borrow_unsigned_int_max_result;
  subc_zero_minus_two_borrow_unsigned_int_max_result = __builtin_subc(0U, 2U, unsigned_int_max, &subc_zero_minus_two_borrow_unsigned_int_max_borrow_out);

  // __builtin_subc(0U, 2U, 4294967295U, &subc_zero_minus_two_borrow_unsigned_int_max_borrow_out) returns 4294967295U.
  if (!(subc_zero_minus_two_borrow_unsigned_int_max_result == unsigned_int_max))
    goto ERROR;

  // __builtin_subc(0U, 2U, 4294967295U, &subc_zero_minus_two_borrow_unsigned_int_max_borrow_out) stores 1U.
  if (!(subc_zero_minus_two_borrow_unsigned_int_max_borrow_out == 1U))
    goto ERROR;


  unsigned int subc_one_minus_zero_borrow_two_borrow_out;
  unsigned int subc_one_minus_zero_borrow_two_result;
  subc_one_minus_zero_borrow_two_result = __builtin_subc(1U, 0U, 2U, &subc_one_minus_zero_borrow_two_borrow_out);

  // __builtin_subc(1U, 0U, 2U, &subc_one_minus_zero_borrow_two_borrow_out) returns 4294967295U.
  if (!(subc_one_minus_zero_borrow_two_result == unsigned_int_max))
    goto ERROR;

  // __builtin_subc(1U, 0U, 2U, &subc_one_minus_zero_borrow_two_borrow_out) stores 1U.
  if (!(subc_one_minus_zero_borrow_two_borrow_out == 1U))
    goto ERROR;


  // Tests for __builtin_subcl.


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

  // __builtin_subcl(0UL, 0UL, 0UL, &subcl_zero_minus_zero_borrow_zero_borrow_out) returns 0UL.
  if (!(subcl_zero_minus_zero_borrow_zero_result == 0UL))
    goto ERROR;

  // __builtin_subcl(0UL, 0UL, 0UL, &subcl_zero_minus_zero_borrow_zero_borrow_out) stores 0UL.
  if (!(subcl_zero_minus_zero_borrow_zero_borrow_out == 0UL))
    goto ERROR;


  unsigned long int subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out;
  unsigned long int subcl_unsigned_long_max_minus_zero_borrow_zero_result;
  subcl_unsigned_long_max_minus_zero_borrow_zero_result = __builtin_subcl(subcl_unsigned_long_max, 0UL, 0UL, &subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out);

  // ILP32: __builtin_subcl(4294967295UL, 0UL, 0UL, &subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out) returns 4294967295UL.
  // LP64: __builtin_subcl(18446744073709551615UL, 0UL, 0UL, &subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out) returns
  // 18446744073709551615UL.
  if (!(subcl_unsigned_long_max_minus_zero_borrow_zero_result == subcl_unsigned_long_max))
    goto ERROR;

  // ILP32: __builtin_subcl(4294967295UL, 0UL, 0UL, &subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out) stores 0UL.
  // LP64: __builtin_subcl(18446744073709551615UL, 0UL, 0UL, &subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out) stores 0UL.
  if (!(subcl_unsigned_long_max_minus_zero_borrow_zero_borrow_out == 0UL))
    goto ERROR;


  unsigned long int subcl_four_minus_two_borrow_one_borrow_out;
  unsigned long int subcl_four_minus_two_borrow_one_result;
  subcl_four_minus_two_borrow_one_result = __builtin_subcl(4UL, 2UL, 1UL, &subcl_four_minus_two_borrow_one_borrow_out);

  // __builtin_subcl(4UL, 2UL, 1UL, &subcl_four_minus_two_borrow_one_borrow_out) returns 1UL.
  if (!(subcl_four_minus_two_borrow_one_result == 1UL))
    goto ERROR;

  // __builtin_subcl(4UL, 2UL, 1UL, &subcl_four_minus_two_borrow_one_borrow_out) stores 0UL.
  if (!(subcl_four_minus_two_borrow_one_borrow_out == 0UL))
    goto ERROR;


  unsigned long int subcl_zero_minus_one_borrow_zero_borrow_out;
  unsigned long int subcl_zero_minus_one_borrow_zero_result;
  subcl_zero_minus_one_borrow_zero_result = __builtin_subcl(0UL, 1UL, 0UL, &subcl_zero_minus_one_borrow_zero_borrow_out);

  // ILP32: __builtin_subcl(0UL, 1UL, 0UL, &subcl_zero_minus_one_borrow_zero_borrow_out) returns 4294967295UL.
  // LP64: __builtin_subcl(0UL, 1UL, 0UL, &subcl_zero_minus_one_borrow_zero_borrow_out) returns 18446744073709551615UL.
  if (!(subcl_zero_minus_one_borrow_zero_result == subcl_unsigned_long_max))
    goto ERROR;

  // __builtin_subcl(0UL, 1UL, 0UL, &subcl_zero_minus_one_borrow_zero_borrow_out) stores 1UL.
  if (!(subcl_zero_minus_one_borrow_zero_borrow_out == 1UL))
    goto ERROR;


  unsigned long int subcl_zero_minus_zero_borrow_one_borrow_out;
  unsigned long int subcl_zero_minus_zero_borrow_one_result;
  subcl_zero_minus_zero_borrow_one_result = __builtin_subcl(0UL, 0UL, 1UL, &subcl_zero_minus_zero_borrow_one_borrow_out);

  // ILP32: __builtin_subcl(0UL, 0UL, 1UL, &subcl_zero_minus_zero_borrow_one_borrow_out) returns 4294967295UL.
  // LP64: __builtin_subcl(0UL, 0UL, 1UL, &subcl_zero_minus_zero_borrow_one_borrow_out) returns 18446744073709551615UL.
  if (!(subcl_zero_minus_zero_borrow_one_result == subcl_unsigned_long_max))
    goto ERROR;

  // __builtin_subcl(0UL, 0UL, 1UL, &subcl_zero_minus_zero_borrow_one_borrow_out) stores 1UL.
  if (!(subcl_zero_minus_zero_borrow_one_borrow_out == 1UL))
    goto ERROR;


  unsigned long int subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out;
  unsigned long int subcl_zero_minus_two_borrow_unsigned_long_max_result;
  subcl_zero_minus_two_borrow_unsigned_long_max_result = __builtin_subcl(0UL, 2UL, subcl_unsigned_long_max, &subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out);

  // ILP32: __builtin_subcl(0UL, 2UL, 4294967295UL, &subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out) returns 4294967295UL.
  // LP64: __builtin_subcl(0UL, 2UL, 18446744073709551615UL, &subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out) returns
  // 18446744073709551615UL.
  if (!(subcl_zero_minus_two_borrow_unsigned_long_max_result == subcl_unsigned_long_max))
    goto ERROR;

  // ILP32: __builtin_subcl(0UL, 2UL, 4294967295UL, &subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out) stores 1UL.
  // LP64: __builtin_subcl(0UL, 2UL, 18446744073709551615UL, &subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out) stores 1UL.
  if (!(subcl_zero_minus_two_borrow_unsigned_long_max_borrow_out == 1UL))
    goto ERROR;


  unsigned long int subcl_one_minus_zero_borrow_two_borrow_out;
  unsigned long int subcl_one_minus_zero_borrow_two_result;
  subcl_one_minus_zero_borrow_two_result = __builtin_subcl(1UL, 0UL, 2UL, &subcl_one_minus_zero_borrow_two_borrow_out);

  // ILP32: __builtin_subcl(1UL, 0UL, 2UL, &subcl_one_minus_zero_borrow_two_borrow_out) returns 4294967295UL.
  // LP64: __builtin_subcl(1UL, 0UL, 2UL, &subcl_one_minus_zero_borrow_two_borrow_out) returns 18446744073709551615UL.
  if (!(subcl_one_minus_zero_borrow_two_result == subcl_unsigned_long_max))
    goto ERROR;

  // __builtin_subcl(1UL, 0UL, 2UL, &subcl_one_minus_zero_borrow_two_borrow_out) stores 1UL.
  if (!(subcl_one_minus_zero_borrow_two_borrow_out == 1UL))
    goto ERROR;


  // Tests for __builtin_subcll.

  unsigned long long int subcll_zero_minus_zero_borrow_zero_borrow_out;
  unsigned long long int subcll_zero_minus_zero_borrow_zero_result;
  subcll_zero_minus_zero_borrow_zero_result = __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_zero_minus_zero_borrow_zero_borrow_out);

  // __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_zero_minus_zero_borrow_zero_borrow_out) returns 0ULL.
  if (!(subcll_zero_minus_zero_borrow_zero_result == 0ULL))
    goto ERROR;

  // __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_zero_minus_zero_borrow_zero_borrow_out) stores 0ULL.
  if (!(subcll_zero_minus_zero_borrow_zero_borrow_out == 0ULL))
    goto ERROR;


  unsigned long long int subcll_unsigned_long_long_max_minus_zero_borrow_zero_borrow_out;
  unsigned long long int subcll_unsigned_long_long_max_minus_zero_borrow_zero_result;
  subcll_unsigned_long_long_max_minus_zero_borrow_zero_result = __builtin_subcll(unsigned_long_long_max, 0ULL, 0ULL, &subcll_unsigned_long_long_max_minus_zero_borrow_zero_borrow_out);

  // __builtin_subcll(18446744073709551615ULL, 0ULL, 0ULL, &subcll_unsigned_long_long_max_minus_zero_borrow_zero_borrow_out) returns
  // 18446744073709551615ULL.
  if (!(subcll_unsigned_long_long_max_minus_zero_borrow_zero_result == unsigned_long_long_max))
    goto ERROR;

  // __builtin_subcll(18446744073709551615ULL, 0ULL, 0ULL, &subcll_unsigned_long_long_max_minus_zero_borrow_zero_borrow_out) stores 0ULL.
  if (!(subcll_unsigned_long_long_max_minus_zero_borrow_zero_borrow_out == 0ULL))
    goto ERROR;


  unsigned long long int subcll_four_minus_two_borrow_one_borrow_out;
  unsigned long long int subcll_four_minus_two_borrow_one_result;
  subcll_four_minus_two_borrow_one_result = __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_four_minus_two_borrow_one_borrow_out);

  // __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_four_minus_two_borrow_one_borrow_out) returns 1ULL.
  if (!(subcll_four_minus_two_borrow_one_result == 1ULL))
    goto ERROR;

  // __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_four_minus_two_borrow_one_borrow_out) stores 0ULL.
  if (!(subcll_four_minus_two_borrow_one_borrow_out == 0ULL))
    goto ERROR;


  unsigned long long int subcll_zero_minus_one_borrow_zero_borrow_out;
  unsigned long long int subcll_zero_minus_one_borrow_zero_result;
  subcll_zero_minus_one_borrow_zero_result = __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_zero_minus_one_borrow_zero_borrow_out);

  // __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_zero_minus_one_borrow_zero_borrow_out) returns 18446744073709551615ULL.
  if (!(subcll_zero_minus_one_borrow_zero_result == unsigned_long_long_max))
    goto ERROR;

  // __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_zero_minus_one_borrow_zero_borrow_out) stores 1ULL.
  if (!(subcll_zero_minus_one_borrow_zero_borrow_out == 1ULL))
    goto ERROR;


  unsigned long long int subcll_zero_minus_zero_borrow_one_borrow_out;
  unsigned long long int subcll_zero_minus_zero_borrow_one_result;
  subcll_zero_minus_zero_borrow_one_result = __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_zero_minus_zero_borrow_one_borrow_out);

  // __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_zero_minus_zero_borrow_one_borrow_out) returns 18446744073709551615ULL.
  if (!(subcll_zero_minus_zero_borrow_one_result == unsigned_long_long_max))
    goto ERROR;

  // __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_zero_minus_zero_borrow_one_borrow_out) stores 1ULL.
  if (!(subcll_zero_minus_zero_borrow_one_borrow_out == 1ULL))
    goto ERROR;


  unsigned long long int subcll_zero_minus_two_borrow_unsigned_long_long_max_borrow_out;
  unsigned long long int subcll_zero_minus_two_borrow_unsigned_long_long_max_result;
  subcll_zero_minus_two_borrow_unsigned_long_long_max_result = __builtin_subcll(0ULL, 2ULL, unsigned_long_long_max, &subcll_zero_minus_two_borrow_unsigned_long_long_max_borrow_out);

  // __builtin_subcll(0ULL, 2ULL, 18446744073709551615ULL, &subcll_zero_minus_two_borrow_unsigned_long_long_max_borrow_out) returns
  // 18446744073709551615ULL.
  if (!(subcll_zero_minus_two_borrow_unsigned_long_long_max_result == unsigned_long_long_max))
    goto ERROR;

  // __builtin_subcll(0ULL, 2ULL, 18446744073709551615ULL, &subcll_zero_minus_two_borrow_unsigned_long_long_max_borrow_out) stores 1ULL.
  if (!(subcll_zero_minus_two_borrow_unsigned_long_long_max_borrow_out == 1ULL))
    goto ERROR;


  unsigned long long int subcll_one_minus_zero_borrow_two_borrow_out;
  unsigned long long int subcll_one_minus_zero_borrow_two_result;
  subcll_one_minus_zero_borrow_two_result = __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_one_minus_zero_borrow_two_borrow_out);

  // __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_one_minus_zero_borrow_two_borrow_out) returns 18446744073709551615ULL.
  if (!(subcll_one_minus_zero_borrow_two_result == unsigned_long_long_max))
    goto ERROR;

  // __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_one_minus_zero_borrow_two_borrow_out) stores 1ULL.
  if (!(subcll_one_minus_zero_borrow_two_borrow_out == 1ULL))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
