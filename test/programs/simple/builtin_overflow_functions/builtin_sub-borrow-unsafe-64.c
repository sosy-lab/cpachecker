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
  const unsigned int unsigned_int_max = 4294967295U;
  const unsigned long long int unsigned_long_long_max = 18446744073709551615ULL;


  // This program targets LP64 and fails its expected verdict under ILP32.

  // LP64: 0UL - 1UL - 4294967295UL stores 18446744069414584320UL and borrows.
  unsigned long int model_subcl_borrow_out;
  unsigned long int model_subcl_result = __builtin_subcl(0UL, 1UL, 4294967295UL, &model_subcl_borrow_out);
  all_expected_checks_fail = all_expected_checks_fail || (model_subcl_result == 0UL);
  all_expected_checks_fail = all_expected_checks_fail || (model_subcl_borrow_out != 1UL);

  // Unsigned int subtract-with-borrow tests.

  unsigned int subc_borrow_out_1;
  unsigned int subc_result_1 = __builtin_subc(0U, 0U, 0U, &subc_borrow_out_1);

  // 0U - 0U - borrow-in 0U = 0; result = 0U and borrow-out = 0U because 0 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subc_result_1 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (subc_borrow_out_1 != 0U);


  unsigned int subc_borrow_out_2;
  unsigned int subc_result_2 = __builtin_subc(unsigned_int_max, 0U, 0U, &subc_borrow_out_2);

  // 4294967295U - 0U - borrow-in 0U = 4294967295; result = 4294967295U and borrow-out = 0U because 4294967295 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subc_result_2 != unsigned_int_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_borrow_out_2 != 0U);


  unsigned int subc_borrow_out_3;
  unsigned int subc_result_3 = __builtin_subc(4U, 2U, 1U, &subc_borrow_out_3);

  // 4U - 2U - borrow-in 1U = 1; result = 1U and borrow-out = 0U because 1 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subc_result_3 != 1U);

  all_expected_checks_fail = all_expected_checks_fail || (subc_borrow_out_3 != 0U);


  unsigned int subc_borrow_out_4;
  unsigned int subc_result_4 = __builtin_subc(0U, 1U, 0U, &subc_borrow_out_4);

  // 0U - 1U - borrow-in 0U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subc_result_4 != unsigned_int_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_borrow_out_4 != 1U);


  unsigned int subc_borrow_out_5;
  unsigned int subc_result_5 = __builtin_subc(0U, 0U, 1U, &subc_borrow_out_5);

  // 0U - 0U - borrow-in 1U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subc_result_5 != unsigned_int_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_borrow_out_5 != 1U);


  unsigned int subc_borrow_out_6;
  unsigned int subc_result_6 = __builtin_subc(0U, 2U, unsigned_int_max, &subc_borrow_out_6);

  // 0U - 2U - borrow-in 4294967295U = -4294967297; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subc_result_6 != unsigned_int_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_borrow_out_6 != 1U);


  unsigned int subc_borrow_out_7;
  unsigned int subc_result_7 = __builtin_subc(1U, 0U, 2U, &subc_borrow_out_7);

  // 1U - 0U - borrow-in 2U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subc_result_7 != unsigned_int_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_borrow_out_7 != 1U);


  // Unsigned long int subtract-with-borrow tests.

  const unsigned long int subcl_unsigned_long_max = ~0UL;


  unsigned long int subcl_borrow_out_1;
  unsigned long int subcl_result_1 = __builtin_subcl(0UL, 0UL, 0UL, &subcl_borrow_out_1);

  // 0UL - 0UL - borrow-in 0UL = 0; result = 0UL and borrow-out = 0UL because 0 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_result_1 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_borrow_out_1 != 0UL);


  unsigned long int subcl_borrow_out_2;
  unsigned long int subcl_result_2 = __builtin_subcl(subcl_unsigned_long_max, 0UL, 0UL, &subcl_borrow_out_2);

  // 18446744073709551615UL - 0UL - borrow-in 0UL = 18446744073709551615; result = 18446744073709551615UL and borrow-out = 0UL because
  // 18446744073709551615 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_result_2 != subcl_unsigned_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_borrow_out_2 != 0UL);


  unsigned long int subcl_borrow_out_3;
  unsigned long int subcl_result_3 = __builtin_subcl(4UL, 2UL, 1UL, &subcl_borrow_out_3);

  // 4UL - 2UL - borrow-in 1UL = 1; result = 1UL and borrow-out = 0UL because 1 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_result_3 != 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_borrow_out_3 != 0UL);


  unsigned long int subcl_borrow_out_4;
  unsigned long int subcl_result_4 = __builtin_subcl(0UL, 1UL, 0UL, &subcl_borrow_out_4);

  // 0UL - 1UL - borrow-in 0UL = -1; stored result = 18446744073709551615UL and borrow-out = 1UL because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_result_4 != subcl_unsigned_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_borrow_out_4 != 1UL);


  unsigned long int subcl_borrow_out_5;
  unsigned long int subcl_result_5 = __builtin_subcl(0UL, 0UL, 1UL, &subcl_borrow_out_5);

  // 0UL - 0UL - borrow-in 1UL = -1; stored result = 18446744073709551615UL and borrow-out = 1UL because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_result_5 != subcl_unsigned_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_borrow_out_5 != 1UL);


  unsigned long int subcl_borrow_out_6;
  unsigned long int subcl_result_6 = __builtin_subcl(0UL, 2UL, subcl_unsigned_long_max, &subcl_borrow_out_6);

  // 0UL - 2UL - borrow-in 18446744073709551615UL = -18446744073709551617; stored result = 18446744073709551615UL and borrow-out = 1UL
  // because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_result_6 != subcl_unsigned_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_borrow_out_6 != 1UL);


  unsigned long int subcl_borrow_out_7;
  unsigned long int subcl_result_7 = __builtin_subcl(1UL, 0UL, 2UL, &subcl_borrow_out_7);

  // 1UL - 0UL - borrow-in 2UL = -1; stored result = 18446744073709551615UL and borrow-out = 1UL because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_result_7 != subcl_unsigned_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_borrow_out_7 != 1UL);


  // Unsigned long long int subtract-with-borrow tests.

  unsigned long long int subcll_borrow_out_1;
  unsigned long long int subcll_result_1 = __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_borrow_out_1);

  // 0ULL - 0ULL - borrow-in 0ULL = 0; result = 0ULL and borrow-out = 0ULL because 0 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_result_1 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_borrow_out_1 != 0ULL);


  unsigned long long int subcll_borrow_out_2;
  unsigned long long int subcll_result_2 = __builtin_subcll(unsigned_long_long_max, 0ULL, 0ULL, &subcll_borrow_out_2);

  // 18446744073709551615ULL - 0ULL - borrow-in 0ULL = 18446744073709551615; result = 18446744073709551615ULL and borrow-out = 0ULL because
  // 18446744073709551615 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_result_2 != unsigned_long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_borrow_out_2 != 0ULL);


  unsigned long long int subcll_borrow_out_3;
  unsigned long long int subcll_result_3 = __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_borrow_out_3);

  // 4ULL - 2ULL - borrow-in 1ULL = 1; result = 1ULL and borrow-out = 0ULL because 1 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_result_3 != 1ULL);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_borrow_out_3 != 0ULL);


  unsigned long long int subcll_borrow_out_4;
  unsigned long long int subcll_result_4 = __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_borrow_out_4);

  // 0ULL - 1ULL - borrow-in 0ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_result_4 != unsigned_long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_borrow_out_4 != 1ULL);


  unsigned long long int subcll_borrow_out_5;
  unsigned long long int subcll_result_5 = __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_borrow_out_5);

  // 0ULL - 0ULL - borrow-in 1ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_result_5 != unsigned_long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_borrow_out_5 != 1ULL);


  unsigned long long int subcll_borrow_out_6;
  unsigned long long int subcll_result_6 = __builtin_subcll(0ULL, 2ULL, unsigned_long_long_max, &subcll_borrow_out_6);

  // 0ULL - 2ULL - borrow-in 18446744073709551615ULL = -18446744073709551617; stored result = 18446744073709551615ULL and borrow-out = 1ULL
  // because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_result_6 != unsigned_long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_borrow_out_6 != 1ULL);


  unsigned long long int subcll_borrow_out_7;
  unsigned long long int subcll_result_7 = __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_borrow_out_7);

  // 1ULL - 0ULL - borrow-in 2ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_result_7 != unsigned_long_long_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_borrow_out_7 != 1ULL);


  // If no expected-value check fails, every deliberately negated check is false.
  if (!(all_expected_checks_fail))
    goto ERROR;

  return 0;

ERROR:
  return 1;
}
