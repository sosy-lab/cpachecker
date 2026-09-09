// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}


int main(void) {
  int all_expected_checks_fail = 0;


  // Fixed typed test values are initialized directly and never modified.
  const unsigned int uint_max = 4294967295U;
  const unsigned long long int ull_max = 18446744073709551615ULL;


  // long is 32 bits in ILP32 and 64 bits in LP64, so this builtin case has model-dependent expected results.
  if (sizeof(long int) == 4U) {
    unsigned long int model_subcl_bout;
    (void)__builtin_subcl(0UL, (unsigned long int)4294967296ULL, 0UL, &model_subcl_bout);

    // ILP32: (unsigned long int)4294967296ULL = 0UL, so 0UL - 0UL - 0UL = 0UL and borrow-out = 0UL.
    all_expected_checks_fail = all_expected_checks_fail || (model_subcl_bout != 0UL);
  } else {
    unsigned long int model_subcl_bout;
    (void)__builtin_subcl(0UL, (unsigned long int)4294967296ULL, 0UL, &model_subcl_bout);

    // LP64: 4294967296UL fits unsigned long, so 0UL - 4294967296UL - 0UL borrows and borrow-out = 1UL.
    all_expected_checks_fail = all_expected_checks_fail || (model_subcl_bout != 1UL);
  }

  // Unsigned int subtract-with-borrow tests.

  unsigned int subc_bout_1;
  unsigned int subc_res_1 = __builtin_subc(0U, 0U, 0U, &subc_bout_1);

  // 0U - 0U - borrow-in 0U = 0; result = 0U and borrow-out = 0U because 0 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subc_res_1 != 0U);

  all_expected_checks_fail = all_expected_checks_fail || (subc_bout_1 != 0U);


  unsigned int subc_bout_2;
  unsigned int subc_res_2 = __builtin_subc(uint_max, 0U, 0U, &subc_bout_2);

  // 4294967295U - 0U - borrow-in 0U = 4294967295; result = 4294967295U and borrow-out = 0U because 4294967295 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subc_res_2 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_bout_2 != 0U);


  unsigned int subc_bout_3;
  unsigned int subc_res_3 = __builtin_subc(4U, 2U, 1U, &subc_bout_3);

  // 4U - 2U - borrow-in 1U = 1; result = 1U and borrow-out = 0U because 1 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subc_res_3 != 1U);

  all_expected_checks_fail = all_expected_checks_fail || (subc_bout_3 != 0U);


  unsigned int subc_bout_4;
  unsigned int subc_res_4 = __builtin_subc(0U, 1U, 0U, &subc_bout_4);

  // 0U - 1U - borrow-in 0U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subc_res_4 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_bout_4 != 1U);


  unsigned int subc_bout_5;
  unsigned int subc_res_5 = __builtin_subc(0U, 0U, 1U, &subc_bout_5);

  // 0U - 0U - borrow-in 1U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subc_res_5 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_bout_5 != 1U);


  unsigned int subc_bout_6;
  unsigned int subc_res_6 = __builtin_subc(0U, 2U, uint_max, &subc_bout_6);

  // 0U - 2U - borrow-in 4294967295U = -4294967297; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subc_res_6 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_bout_6 != 1U);


  unsigned int subc_bout_7;
  unsigned int subc_res_7 = __builtin_subc(1U, 0U, 2U, &subc_bout_7);

  // 1U - 0U - borrow-in 2U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subc_res_7 != uint_max);

  all_expected_checks_fail = all_expected_checks_fail || (subc_bout_7 != 1U);


  // Unsigned long int subtract-with-borrow tests.

  const unsigned long int subcl_max = ~0UL;


  unsigned long int subcl_bout_1;
  unsigned long int subcl_res_1 = __builtin_subcl(0UL, 0UL, 0UL, &subcl_bout_1);

  // 0UL - 0UL - borrow-in 0UL = 0; result = 0UL and borrow-out = 0UL because 0 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_res_1 != 0UL);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_bout_1 != 0UL);


  unsigned long int subcl_bout_2;
  unsigned long int subcl_res_2 = __builtin_subcl(subcl_max, 0UL, 0UL, &subcl_bout_2);

  // ILP32: 4294967295UL - 0UL - borrow-in 0UL = 4294967295; result = 4294967295UL and borrow-out = 0UL because 4294967295 >= 0.
  // LP64: 18446744073709551615UL - 0UL - borrow-in 0UL = 18446744073709551615; result = 18446744073709551615UL and borrow-out = 0UL because
  // 18446744073709551615 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_res_2 != subcl_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_bout_2 != 0UL);


  unsigned long int subcl_bout_3;
  unsigned long int subcl_res_3 = __builtin_subcl(4UL, 2UL, 1UL, &subcl_bout_3);

  // 4UL - 2UL - borrow-in 1UL = 1; result = 1UL and borrow-out = 0UL because 1 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_res_3 != 1UL);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_bout_3 != 0UL);


  unsigned long int subcl_bout_4;
  unsigned long int subcl_res_4 = __builtin_subcl(0UL, 1UL, 0UL, &subcl_bout_4);

  // ILP32: 0UL - 1UL - borrow-in 0UL = -1; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage underflows.
  // LP64: 0UL - 1UL - borrow-in 0UL = -1; stored result = 18446744073709551615UL and borrow-out = 1UL because a subtraction stage
  // underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_res_4 != subcl_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_bout_4 != 1UL);


  unsigned long int subcl_bout_5;
  unsigned long int subcl_res_5 = __builtin_subcl(0UL, 0UL, 1UL, &subcl_bout_5);

  // ILP32: 0UL - 0UL - borrow-in 1UL = -1; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage underflows.
  // LP64: 0UL - 0UL - borrow-in 1UL = -1; stored result = 18446744073709551615UL and borrow-out = 1UL because a subtraction stage
  // underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_res_5 != subcl_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_bout_5 != 1UL);


  unsigned long int subcl_bout_6;
  unsigned long int subcl_res_6 = __builtin_subcl(0UL, 2UL, subcl_max, &subcl_bout_6);

  // ILP32: 0UL - 2UL - borrow-in 4294967295UL = -4294967297; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage
  // underflows.
  // LP64: 0UL - 2UL - borrow-in 18446744073709551615UL = -18446744073709551617; stored result = 18446744073709551615UL and borrow-out = 1UL
  // because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_res_6 != subcl_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_bout_6 != 1UL);


  unsigned long int subcl_bout_7;
  unsigned long int subcl_res_7 = __builtin_subcl(1UL, 0UL, 2UL, &subcl_bout_7);

  // ILP32: 1UL - 0UL - borrow-in 2UL = -1; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage underflows.
  // LP64: 1UL - 0UL - borrow-in 2UL = -1; stored result = 18446744073709551615UL and borrow-out = 1UL because a subtraction stage
  // underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcl_res_7 != subcl_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcl_bout_7 != 1UL);


  // Unsigned long long int subtract-with-borrow tests.

  unsigned long long int subcll_bout_1;
  unsigned long long int subcll_res_1 = __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_bout_1);

  // 0ULL - 0ULL - borrow-in 0ULL = 0; result = 0ULL and borrow-out = 0ULL because 0 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_res_1 != 0ULL);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_bout_1 != 0ULL);


  unsigned long long int subcll_bout_2;
  unsigned long long int subcll_res_2 = __builtin_subcll(ull_max, 0ULL, 0ULL, &subcll_bout_2);

  // 18446744073709551615ULL - 0ULL - borrow-in 0ULL = 18446744073709551615; result = 18446744073709551615ULL and borrow-out = 0ULL because
  // 18446744073709551615 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_res_2 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_bout_2 != 0ULL);


  unsigned long long int subcll_bout_3;
  unsigned long long int subcll_res_3 = __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_bout_3);

  // 4ULL - 2ULL - borrow-in 1ULL = 1; result = 1ULL and borrow-out = 0ULL because 1 >= 0.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_res_3 != 1ULL);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_bout_3 != 0ULL);


  unsigned long long int subcll_bout_4;
  unsigned long long int subcll_res_4 = __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_bout_4);

  // 0ULL - 1ULL - borrow-in 0ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_res_4 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_bout_4 != 1ULL);


  unsigned long long int subcll_bout_5;
  unsigned long long int subcll_res_5 = __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_bout_5);

  // 0ULL - 0ULL - borrow-in 1ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_res_5 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_bout_5 != 1ULL);


  unsigned long long int subcll_bout_6;
  unsigned long long int subcll_res_6 = __builtin_subcll(0ULL, 2ULL, ull_max, &subcll_bout_6);

  // 0ULL - 2ULL - borrow-in 18446744073709551615ULL = -18446744073709551617; stored result = 18446744073709551615ULL and borrow-out = 1ULL
  // because a subtraction stage underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_res_6 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_bout_6 != 1ULL);


  unsigned long long int subcll_bout_7;
  unsigned long long int subcll_res_7 = __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_bout_7);

  // 1ULL - 0ULL - borrow-in 2ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  all_expected_checks_fail = all_expected_checks_fail || (subcll_res_7 != ull_max);

  all_expected_checks_fail = all_expected_checks_fail || (subcll_bout_7 != 1ULL);


  // If no expected-value check fails, every deliberately negated check is false.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
