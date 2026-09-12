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


// This program is safe for unreach-label, valid-memsafety, and no-overflow in ILP32 and LP64
int main(void) {

  // Constant test values are initialized directly and never modified.
  const unsigned int uint_max = 4294967295U;
  const unsigned long long int ull_max = 18446744073709551615ULL;
  const unsigned long int subcl_max = ~0UL;


  // long is 32 bits in ILP32 and 64 bits in LP64, so this builtin case has model-dependent expected results.
  if (sizeof(long int) == 4U) {
    unsigned long int model_subcl_bout;
    (void)__builtin_subcl(0UL, (unsigned long int)4294967296ULL, 0UL, &model_subcl_bout);

    // ILP32: (unsigned long int)4294967296ULL = 0UL, so 0UL - 0UL - 0UL = 0UL and borrow-out = 0UL.
    __VERIFIER_assert(model_subcl_bout == 0UL);
  } else {
    unsigned long int model_subcl_bout;
    (void)__builtin_subcl(0UL, (unsigned long int)4294967296ULL, 0UL, &model_subcl_bout);

    // LP64: 4294967296UL fits unsigned long, so 0UL - 4294967296UL - 0UL borrows and borrow-out = 1UL.
    __VERIFIER_assert(model_subcl_bout == 1UL);
  }

  // Unsigned int subtract-with-borrow tests.

  unsigned int subc_0_0_bin0_bout;
  unsigned int subc_0_0_bin0_res;
  subc_0_0_bin0_res = __builtin_subc(0U, 0U, 0U, &subc_0_0_bin0_bout);

  // 0U - 0U - borrow-in 0U = 0; result = 0U and borrow-out = 0U because 0 >= 0.
  __VERIFIER_assert(subc_0_0_bin0_res == 0U);

  __VERIFIER_assert(subc_0_0_bin0_bout == 0U);


  unsigned int subc_max_0_bin0_bout;
  unsigned int subc_max_0_bin0_res;
  subc_max_0_bin0_res = __builtin_subc(uint_max, 0U, 0U, &subc_max_0_bin0_bout);

  // 4294967295U - 0U - borrow-in 0U = 4294967295; result = 4294967295U and borrow-out = 0U because 4294967295 >= 0.
  __VERIFIER_assert(subc_max_0_bin0_res == uint_max);

  __VERIFIER_assert(subc_max_0_bin0_bout == 0U);


  unsigned int subc_4_2_bin1_bout;
  unsigned int subc_4_2_bin1_res;
  subc_4_2_bin1_res = __builtin_subc(4U, 2U, 1U, &subc_4_2_bin1_bout);

  // 4U - 2U - borrow-in 1U = 1; result = 1U and borrow-out = 0U because 1 >= 0.
  __VERIFIER_assert(subc_4_2_bin1_res == 1U);

  __VERIFIER_assert(subc_4_2_bin1_bout == 0U);


  unsigned int subc_0_1_bin0_bout;
  unsigned int subc_0_1_bin0_res;
  subc_0_1_bin0_res = __builtin_subc(0U, 1U, 0U, &subc_0_1_bin0_bout);

  // 0U - 1U - borrow-in 0U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  __VERIFIER_assert(subc_0_1_bin0_res == uint_max);

  __VERIFIER_assert(subc_0_1_bin0_bout == 1U);


  unsigned int subc_0_0_bin1_bout;
  unsigned int subc_0_0_bin1_res;
  subc_0_0_bin1_res = __builtin_subc(0U, 0U, 1U, &subc_0_0_bin1_bout);

  // 0U - 0U - borrow-in 1U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  __VERIFIER_assert(subc_0_0_bin1_res == uint_max);

  __VERIFIER_assert(subc_0_0_bin1_bout == 1U);


  unsigned int subc_0_2_binmax_bout;
  unsigned int subc_0_2_binmax_res;
  subc_0_2_binmax_res = __builtin_subc(0U, 2U, uint_max, &subc_0_2_binmax_bout);

  // 0U - 2U - borrow-in 4294967295U = -4294967297; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  __VERIFIER_assert(subc_0_2_binmax_res == uint_max);

  __VERIFIER_assert(subc_0_2_binmax_bout == 1U);


  unsigned int subc_1_0_bin2_bout;
  unsigned int subc_1_0_bin2_res;
  subc_1_0_bin2_res = __builtin_subc(1U, 0U, 2U, &subc_1_0_bin2_bout);

  // 1U - 0U - borrow-in 2U = -1; stored result = 4294967295U and borrow-out = 1U because a subtraction stage underflows.
  __VERIFIER_assert(subc_1_0_bin2_res == uint_max);

  __VERIFIER_assert(subc_1_0_bin2_bout == 1U);
  unsigned long int subcl_0_0_bin0_bout;
  unsigned long int subcl_0_0_bin0_res;
  subcl_0_0_bin0_res = __builtin_subcl(0UL, 0UL, 0UL, &subcl_0_0_bin0_bout);

  // 0UL - 0UL - borrow-in 0UL = 0; result = 0UL and borrow-out = 0UL because 0 >= 0.
  __VERIFIER_assert(subcl_0_0_bin0_res == 0UL);

  __VERIFIER_assert(subcl_0_0_bin0_bout == 0UL);


  unsigned long int subcl_max_0_bin0_bout;
  unsigned long int subcl_max_0_bin0_res;
  subcl_max_0_bin0_res = __builtin_subcl(subcl_max, 0UL, 0UL, &subcl_max_0_bin0_bout);

  // ILP32: 4294967295UL - 0UL - borrow-in 0UL = 4294967295; result = 4294967295UL and borrow-out = 0UL because 4294967295 >= 0.
  // LP64: 18446744073709551615UL - 0UL - borrow-in 0UL = 18446744073709551615; result = 18446744073709551615UL and borrow-out = 0UL because
  // 18446744073709551615 >= 0.
  __VERIFIER_assert(subcl_max_0_bin0_res == subcl_max);

  __VERIFIER_assert(subcl_max_0_bin0_bout == 0UL);


  unsigned long int subcl_4_2_bin1_bout;
  unsigned long int subcl_4_2_bin1_res;
  subcl_4_2_bin1_res = __builtin_subcl(4UL, 2UL, 1UL, &subcl_4_2_bin1_bout);

  // 4UL - 2UL - borrow-in 1UL = 1; result = 1UL and borrow-out = 0UL because 1 >= 0.
  __VERIFIER_assert(subcl_4_2_bin1_res == 1UL);

  __VERIFIER_assert(subcl_4_2_bin1_bout == 0UL);


  unsigned long int subcl_0_1_bin0_bout;
  unsigned long int subcl_0_1_bin0_res;
  subcl_0_1_bin0_res = __builtin_subcl(0UL, 1UL, 0UL, &subcl_0_1_bin0_bout);

  // ILP32: 0UL - 1UL - borrow-in 0UL = -1; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage underflows.
  // LP64: 0UL - 1UL - borrow-in 0UL = -1; stored result = 18446744073709551615UL and borrow-out = 1UL because a subtraction stage
  // underflows.
  __VERIFIER_assert(subcl_0_1_bin0_res == subcl_max);

  __VERIFIER_assert(subcl_0_1_bin0_bout == 1UL);


  unsigned long int subcl_0_0_bin1_bout;
  unsigned long int subcl_0_0_bin1_res;
  subcl_0_0_bin1_res = __builtin_subcl(0UL, 0UL, 1UL, &subcl_0_0_bin1_bout);

  // ILP32: 0UL - 0UL - borrow-in 1UL = -1; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage underflows.
  // LP64: 0UL - 0UL - borrow-in 1UL = -1; stored result = 18446744073709551615UL and borrow-out = 1UL because a subtraction stage
  // underflows.
  __VERIFIER_assert(subcl_0_0_bin1_res == subcl_max);

  __VERIFIER_assert(subcl_0_0_bin1_bout == 1UL);


  unsigned long int subcl_0_2_binmax_bout;
  unsigned long int subcl_0_2_binmax_res;
  subcl_0_2_binmax_res = __builtin_subcl(0UL, 2UL, subcl_max, &subcl_0_2_binmax_bout);

  // ILP32: 0UL - 2UL - borrow-in 4294967295UL = -4294967297; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage
  // underflows.
  // LP64: 0UL - 2UL - borrow-in 18446744073709551615UL = -18446744073709551617; stored result = 18446744073709551615UL and borrow-out = 1UL
  // because a subtraction stage underflows.
  __VERIFIER_assert(subcl_0_2_binmax_res == subcl_max);

  __VERIFIER_assert(subcl_0_2_binmax_bout == 1UL);


  unsigned long int subcl_1_0_bin2_bout;
  unsigned long int subcl_1_0_bin2_res;
  subcl_1_0_bin2_res = __builtin_subcl(1UL, 0UL, 2UL, &subcl_1_0_bin2_bout);

  // ILP32: 1UL - 0UL - borrow-in 2UL = -1; stored result = 4294967295UL and borrow-out = 1UL because a subtraction stage underflows.
  // LP64: 1UL - 0UL - borrow-in 2UL = -1; stored result = 18446744073709551615UL and borrow-out = 1UL because a subtraction stage
  // underflows.
  __VERIFIER_assert(subcl_1_0_bin2_res == subcl_max);

  __VERIFIER_assert(subcl_1_0_bin2_bout == 1UL);


  // Unsigned long long int subtract-with-borrow tests.

  unsigned long long int subcll_0_0_bin0_bout;
  unsigned long long int subcll_0_0_bin0_res;
  subcll_0_0_bin0_res = __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_0_0_bin0_bout);

  // 0ULL - 0ULL - borrow-in 0ULL = 0; result = 0ULL and borrow-out = 0ULL because 0 >= 0.
  __VERIFIER_assert(subcll_0_0_bin0_res == 0ULL);

  __VERIFIER_assert(subcll_0_0_bin0_bout == 0ULL);


  unsigned long long int subcll_max_0_bin0_bout;
  unsigned long long int subcll_max_0_bin0_res;
  subcll_max_0_bin0_res = __builtin_subcll(ull_max, 0ULL, 0ULL, &subcll_max_0_bin0_bout);

  // 18446744073709551615ULL - 0ULL - borrow-in 0ULL = 18446744073709551615; result = 18446744073709551615ULL and borrow-out = 0ULL because
  // 18446744073709551615 >= 0.
  __VERIFIER_assert(subcll_max_0_bin0_res == ull_max);

  __VERIFIER_assert(subcll_max_0_bin0_bout == 0ULL);


  unsigned long long int subcll_4_2_bin1_bout;
  unsigned long long int subcll_4_2_bin1_res;
  subcll_4_2_bin1_res = __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_4_2_bin1_bout);

  // 4ULL - 2ULL - borrow-in 1ULL = 1; result = 1ULL and borrow-out = 0ULL because 1 >= 0.
  __VERIFIER_assert(subcll_4_2_bin1_res == 1ULL);

  __VERIFIER_assert(subcll_4_2_bin1_bout == 0ULL);


  unsigned long long int subcll_0_1_bin0_bout;
  unsigned long long int subcll_0_1_bin0_res;
  subcll_0_1_bin0_res = __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_0_1_bin0_bout);

  // 0ULL - 1ULL - borrow-in 0ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  __VERIFIER_assert(subcll_0_1_bin0_res == ull_max);

  __VERIFIER_assert(subcll_0_1_bin0_bout == 1ULL);


  unsigned long long int subcll_0_0_bin1_bout;
  unsigned long long int subcll_0_0_bin1_res;
  subcll_0_0_bin1_res = __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_0_0_bin1_bout);

  // 0ULL - 0ULL - borrow-in 1ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  __VERIFIER_assert(subcll_0_0_bin1_res == ull_max);

  __VERIFIER_assert(subcll_0_0_bin1_bout == 1ULL);


  unsigned long long int subcll_0_2_binmax_bout;
  unsigned long long int subcll_0_2_binmax_res;
  subcll_0_2_binmax_res = __builtin_subcll(0ULL, 2ULL, ull_max, &subcll_0_2_binmax_bout);

  // 0ULL - 2ULL - borrow-in 18446744073709551615ULL = -18446744073709551617; stored result = 18446744073709551615ULL and borrow-out = 1ULL
  // because a subtraction stage underflows.
  __VERIFIER_assert(subcll_0_2_binmax_res == ull_max);

  __VERIFIER_assert(subcll_0_2_binmax_bout == 1ULL);


  unsigned long long int subcll_1_0_bin2_bout;
  unsigned long long int subcll_1_0_bin2_res;
  subcll_1_0_bin2_res = __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_1_0_bin2_bout);

  // 1ULL - 0ULL - borrow-in 2ULL = -1; stored result = 18446744073709551615ULL and borrow-out = 1ULL because a subtraction stage
  // underflows.
  __VERIFIER_assert(subcll_1_0_bin2_res == ull_max);

  __VERIFIER_assert(subcll_1_0_bin2_bout == 1ULL);


  return 0;
}
