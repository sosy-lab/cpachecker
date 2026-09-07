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

  // Fixed typed test values are initialized directly and never modified.
  const unsigned int unsigned_int_max = 4294967295U;
  const unsigned long long int unsigned_long_long_max = 18446744073709551615ULL;


  // Tests for __builtin_subc.

  unsigned int subc_borrow_out_1;
  unsigned int subc_result_1 = __builtin_subc(0U, 0U, 0U, &subc_borrow_out_1);

  // __builtin_subc(0U, 0U, 0U, &subc_borrow_out_1) returns 0U.
  __VERIFIER_assert(subc_result_1 == 0U);

  // __builtin_subc(0U, 0U, 0U, &subc_borrow_out_1) stores 0U.
  __VERIFIER_assert(subc_borrow_out_1 == 0U);


  unsigned int subc_borrow_out_2;
  unsigned int subc_result_2 = __builtin_subc(unsigned_int_max, 0U, 0U, &subc_borrow_out_2);

  // __builtin_subc(4294967295U, 0U, 0U, &subc_borrow_out_2) returns 4294967295U.
  __VERIFIER_assert(subc_result_2 == unsigned_int_max);

  // __builtin_subc(4294967295U, 0U, 0U, &subc_borrow_out_2) stores 0U.
  __VERIFIER_assert(subc_borrow_out_2 == 0U);


  unsigned int subc_borrow_out_3;
  unsigned int subc_result_3 = __builtin_subc(4U, 2U, 1U, &subc_borrow_out_3);

  // __builtin_subc(4U, 2U, 1U, &subc_borrow_out_3) returns 1U.
  __VERIFIER_assert(subc_result_3 == 1U);

  // __builtin_subc(4U, 2U, 1U, &subc_borrow_out_3) stores 0U.
  __VERIFIER_assert(subc_borrow_out_3 == 0U);


  unsigned int subc_borrow_out_4;
  unsigned int subc_result_4 = __builtin_subc(0U, 1U, 0U, &subc_borrow_out_4);

  // __builtin_subc(0U, 1U, 0U, &subc_borrow_out_4) returns 4294967295U.
  __VERIFIER_assert(subc_result_4 == unsigned_int_max);

  // __builtin_subc(0U, 1U, 0U, &subc_borrow_out_4) stores 1U.
  __VERIFIER_assert(subc_borrow_out_4 == 1U);


  unsigned int subc_borrow_out_5;
  unsigned int subc_result_5 = __builtin_subc(0U, 0U, 1U, &subc_borrow_out_5);

  // __builtin_subc(0U, 0U, 1U, &subc_borrow_out_5) returns 4294967295U.
  __VERIFIER_assert(subc_result_5 == unsigned_int_max);

  // __builtin_subc(0U, 0U, 1U, &subc_borrow_out_5) stores 1U.
  __VERIFIER_assert(subc_borrow_out_5 == 1U);


  unsigned int subc_borrow_out_6;
  unsigned int subc_result_6 = __builtin_subc(0U, 2U, unsigned_int_max, &subc_borrow_out_6);

  // __builtin_subc(0U, 2U, 4294967295U, &subc_borrow_out_6) returns 4294967295U.
  __VERIFIER_assert(subc_result_6 == unsigned_int_max);

  // __builtin_subc(0U, 2U, 4294967295U, &subc_borrow_out_6) stores 1U.
  __VERIFIER_assert(subc_borrow_out_6 == 1U);


  unsigned int subc_borrow_out_7;
  unsigned int subc_result_7 = __builtin_subc(1U, 0U, 2U, &subc_borrow_out_7);

  // __builtin_subc(1U, 0U, 2U, &subc_borrow_out_7) returns 4294967295U.
  __VERIFIER_assert(subc_result_7 == unsigned_int_max);

  // __builtin_subc(1U, 0U, 2U, &subc_borrow_out_7) stores 1U.
  __VERIFIER_assert(subc_borrow_out_7 == 1U);


  // Tests for __builtin_subcl.

  const unsigned long int subcl_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  __VERIFIER_assert((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)subcl_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)subcl_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max));


  unsigned long int subcl_borrow_out_1;
  unsigned long int subcl_result_1 = __builtin_subcl(0UL, 0UL, 0UL, &subcl_borrow_out_1);

  // __builtin_subcl(0UL, 0UL, 0UL, &subcl_borrow_out_1) returns 0UL.
  __VERIFIER_assert(subcl_result_1 == 0UL);

  // __builtin_subcl(0UL, 0UL, 0UL, &subcl_borrow_out_1) stores 0UL.
  __VERIFIER_assert(subcl_borrow_out_1 == 0UL);


  unsigned long int subcl_borrow_out_2;
  unsigned long int subcl_result_2 = __builtin_subcl(subcl_unsigned_long_max, 0UL, 0UL, &subcl_borrow_out_2);

  // ILP32: __builtin_subcl(4294967295UL, 0UL, 0UL, &subcl_borrow_out_2) returns 4294967295UL.
  // LP64: __builtin_subcl(18446744073709551615UL, 0UL, 0UL, &subcl_borrow_out_2) returns 18446744073709551615UL.
  __VERIFIER_assert(subcl_result_2 == subcl_unsigned_long_max);

  // ILP32: __builtin_subcl(4294967295UL, 0UL, 0UL, &subcl_borrow_out_2) stores 0UL.
  // LP64: __builtin_subcl(18446744073709551615UL, 0UL, 0UL, &subcl_borrow_out_2) stores 0UL.
  __VERIFIER_assert(subcl_borrow_out_2 == 0UL);


  unsigned long int subcl_borrow_out_3;
  unsigned long int subcl_result_3 = __builtin_subcl(4UL, 2UL, 1UL, &subcl_borrow_out_3);

  // __builtin_subcl(4UL, 2UL, 1UL, &subcl_borrow_out_3) returns 1UL.
  __VERIFIER_assert(subcl_result_3 == 1UL);

  // __builtin_subcl(4UL, 2UL, 1UL, &subcl_borrow_out_3) stores 0UL.
  __VERIFIER_assert(subcl_borrow_out_3 == 0UL);


  unsigned long int subcl_borrow_out_4;
  unsigned long int subcl_result_4 = __builtin_subcl(0UL, 1UL, 0UL, &subcl_borrow_out_4);

  // ILP32: __builtin_subcl(0UL, 1UL, 0UL, &subcl_borrow_out_4) returns 4294967295UL.
  // LP64: __builtin_subcl(0UL, 1UL, 0UL, &subcl_borrow_out_4) returns 18446744073709551615UL.
  __VERIFIER_assert(subcl_result_4 == subcl_unsigned_long_max);

  // __builtin_subcl(0UL, 1UL, 0UL, &subcl_borrow_out_4) stores 1UL.
  __VERIFIER_assert(subcl_borrow_out_4 == 1UL);


  unsigned long int subcl_borrow_out_5;
  unsigned long int subcl_result_5 = __builtin_subcl(0UL, 0UL, 1UL, &subcl_borrow_out_5);

  // ILP32: __builtin_subcl(0UL, 0UL, 1UL, &subcl_borrow_out_5) returns 4294967295UL.
  // LP64: __builtin_subcl(0UL, 0UL, 1UL, &subcl_borrow_out_5) returns 18446744073709551615UL.
  __VERIFIER_assert(subcl_result_5 == subcl_unsigned_long_max);

  // __builtin_subcl(0UL, 0UL, 1UL, &subcl_borrow_out_5) stores 1UL.
  __VERIFIER_assert(subcl_borrow_out_5 == 1UL);


  unsigned long int subcl_borrow_out_6;
  unsigned long int subcl_result_6 = __builtin_subcl(0UL, 2UL, subcl_unsigned_long_max, &subcl_borrow_out_6);

  // ILP32: __builtin_subcl(0UL, 2UL, 4294967295UL, &subcl_borrow_out_6) returns 4294967295UL.
  // LP64: __builtin_subcl(0UL, 2UL, 18446744073709551615UL, &subcl_borrow_out_6) returns 18446744073709551615UL.
  __VERIFIER_assert(subcl_result_6 == subcl_unsigned_long_max);

  // ILP32: __builtin_subcl(0UL, 2UL, 4294967295UL, &subcl_borrow_out_6) stores 1UL.
  // LP64: __builtin_subcl(0UL, 2UL, 18446744073709551615UL, &subcl_borrow_out_6) stores 1UL.
  __VERIFIER_assert(subcl_borrow_out_6 == 1UL);


  unsigned long int subcl_borrow_out_7;
  unsigned long int subcl_result_7 = __builtin_subcl(1UL, 0UL, 2UL, &subcl_borrow_out_7);

  // ILP32: __builtin_subcl(1UL, 0UL, 2UL, &subcl_borrow_out_7) returns 4294967295UL.
  // LP64: __builtin_subcl(1UL, 0UL, 2UL, &subcl_borrow_out_7) returns 18446744073709551615UL.
  __VERIFIER_assert(subcl_result_7 == subcl_unsigned_long_max);

  // __builtin_subcl(1UL, 0UL, 2UL, &subcl_borrow_out_7) stores 1UL.
  __VERIFIER_assert(subcl_borrow_out_7 == 1UL);


  // Tests for __builtin_subcll.

  unsigned long long int subcll_borrow_out_1;
  unsigned long long int subcll_result_1 = __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_borrow_out_1);

  // __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_borrow_out_1) returns 0ULL.
  __VERIFIER_assert(subcll_result_1 == 0ULL);

  // __builtin_subcll(0ULL, 0ULL, 0ULL, &subcll_borrow_out_1) stores 0ULL.
  __VERIFIER_assert(subcll_borrow_out_1 == 0ULL);


  unsigned long long int subcll_borrow_out_2;
  unsigned long long int subcll_result_2 = __builtin_subcll(unsigned_long_long_max, 0ULL, 0ULL, &subcll_borrow_out_2);

  // __builtin_subcll(18446744073709551615ULL, 0ULL, 0ULL, &subcll_borrow_out_2) returns 18446744073709551615ULL.
  __VERIFIER_assert(subcll_result_2 == unsigned_long_long_max);

  // __builtin_subcll(18446744073709551615ULL, 0ULL, 0ULL, &subcll_borrow_out_2) stores 0ULL.
  __VERIFIER_assert(subcll_borrow_out_2 == 0ULL);


  unsigned long long int subcll_borrow_out_3;
  unsigned long long int subcll_result_3 = __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_borrow_out_3);

  // __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_borrow_out_3) returns 1ULL.
  __VERIFIER_assert(subcll_result_3 == 1ULL);

  // __builtin_subcll(4ULL, 2ULL, 1ULL, &subcll_borrow_out_3) stores 0ULL.
  __VERIFIER_assert(subcll_borrow_out_3 == 0ULL);


  unsigned long long int subcll_borrow_out_4;
  unsigned long long int subcll_result_4 = __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_borrow_out_4);

  // __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_borrow_out_4) returns 18446744073709551615ULL.
  __VERIFIER_assert(subcll_result_4 == unsigned_long_long_max);

  // __builtin_subcll(0ULL, 1ULL, 0ULL, &subcll_borrow_out_4) stores 1ULL.
  __VERIFIER_assert(subcll_borrow_out_4 == 1ULL);


  unsigned long long int subcll_borrow_out_5;
  unsigned long long int subcll_result_5 = __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_borrow_out_5);

  // __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_borrow_out_5) returns 18446744073709551615ULL.
  __VERIFIER_assert(subcll_result_5 == unsigned_long_long_max);

  // __builtin_subcll(0ULL, 0ULL, 1ULL, &subcll_borrow_out_5) stores 1ULL.
  __VERIFIER_assert(subcll_borrow_out_5 == 1ULL);


  unsigned long long int subcll_borrow_out_6;
  unsigned long long int subcll_result_6 = __builtin_subcll(0ULL, 2ULL, unsigned_long_long_max, &subcll_borrow_out_6);

  // __builtin_subcll(0ULL, 2ULL, 18446744073709551615ULL, &subcll_borrow_out_6) returns 18446744073709551615ULL.
  __VERIFIER_assert(subcll_result_6 == unsigned_long_long_max);

  // __builtin_subcll(0ULL, 2ULL, 18446744073709551615ULL, &subcll_borrow_out_6) stores 1ULL.
  __VERIFIER_assert(subcll_borrow_out_6 == 1ULL);


  unsigned long long int subcll_borrow_out_7;
  unsigned long long int subcll_result_7 = __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_borrow_out_7);

  // __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_borrow_out_7) returns 18446744073709551615ULL.
  __VERIFIER_assert(subcll_result_7 == unsigned_long_long_max);

  // __builtin_subcll(1ULL, 0ULL, 2ULL, &subcll_borrow_out_7) stores 1ULL.
  __VERIFIER_assert(subcll_borrow_out_7 == 1ULL);
  return 0;
}
