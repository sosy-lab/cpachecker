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


  // Tests for __builtin_addc.

  unsigned int addc_carry_out_1;
  unsigned int addc_result_1 = __builtin_addc(0U, 0U, 0U, &addc_carry_out_1);

  // __builtin_addc(0U, 0U, 0U, &addc_carry_out_1) returns 0U.
  __VERIFIER_assert(addc_result_1 == 0U);

  // __builtin_addc(0U, 0U, 0U, &addc_carry_out_1) stores 0U.
  __VERIFIER_assert(addc_carry_out_1 == 0U);


  unsigned int addc_carry_out_2;
  unsigned int addc_result_2 = __builtin_addc(unsigned_int_max, 0U, 0U, &addc_carry_out_2);

  // __builtin_addc(4294967295U, 0U, 0U, &addc_carry_out_2) returns 4294967295U.
  __VERIFIER_assert(addc_result_2 == unsigned_int_max);

  // __builtin_addc(4294967295U, 0U, 0U, &addc_carry_out_2) stores 0U.
  __VERIFIER_assert(addc_carry_out_2 == 0U);


  unsigned int addc_carry_out_3;
  unsigned int addc_result_3 = __builtin_addc(1U, 2U, 1U, &addc_carry_out_3);

  // __builtin_addc(1U, 2U, 1U, &addc_carry_out_3) returns 4U.
  __VERIFIER_assert(addc_result_3 == 4U);

  // __builtin_addc(1U, 2U, 1U, &addc_carry_out_3) stores 0U.
  __VERIFIER_assert(addc_carry_out_3 == 0U);


  unsigned int addc_carry_out_4;
  unsigned int addc_result_4 = __builtin_addc(unsigned_int_max, 1U, 0U, &addc_carry_out_4);

  // __builtin_addc(4294967295U, 1U, 0U, &addc_carry_out_4) returns 0U.
  __VERIFIER_assert(addc_result_4 == 0U);

  // __builtin_addc(4294967295U, 1U, 0U, &addc_carry_out_4) stores 1U.
  __VERIFIER_assert(addc_carry_out_4 == 1U);


  unsigned int addc_carry_out_5;
  unsigned int addc_result_5 = __builtin_addc(unsigned_int_max, 0U, 1U, &addc_carry_out_5);

  // __builtin_addc(4294967295U, 0U, 1U, &addc_carry_out_5) returns 0U.
  __VERIFIER_assert(addc_result_5 == 0U);

  // __builtin_addc(4294967295U, 0U, 1U, &addc_carry_out_5) stores 1U.
  __VERIFIER_assert(addc_carry_out_5 == 1U);


  unsigned int addc_carry_out_6;
  unsigned int addc_result_6 = __builtin_addc(unsigned_int_max, unsigned_int_max, 2U, &addc_carry_out_6);

  // __builtin_addc(4294967295U, 4294967295U, 2U, &addc_carry_out_6) returns 0U.
  __VERIFIER_assert(addc_result_6 == 0U);

  // __builtin_addc(4294967295U, 4294967295U, 2U, &addc_carry_out_6) stores 1U.
  __VERIFIER_assert(addc_carry_out_6 == 1U);


  unsigned int addc_carry_out_7;
  unsigned int addc_result_7 = __builtin_addc(1U, 0U, unsigned_int_max, &addc_carry_out_7);

  // __builtin_addc(1U, 0U, 4294967295U, &addc_carry_out_7) returns 0U.
  __VERIFIER_assert(addc_result_7 == 0U);

  // __builtin_addc(1U, 0U, 4294967295U, &addc_carry_out_7) stores 1U.
  __VERIFIER_assert(addc_carry_out_7 == 1U);


  // Tests for __builtin_addcl.

  const unsigned long int addcl_unsigned_long_max = ~0UL;

  // ILP32: The calculated unsigned long must equal unsigned int maximum 4294967295U.
  // LP64: The calculated unsigned long must equal unsigned long long maximum 18446744073709551615ULL.
  __VERIFIER_assert((sizeof(unsigned long int) == sizeof(unsigned int) &&
          (unsigned int)addcl_unsigned_long_max == unsigned_int_max) ||
         (sizeof(unsigned long int) == sizeof(unsigned long long int) &&
          (unsigned long long int)addcl_unsigned_long_max ==
            (unsigned long long int)(unsigned long int)unsigned_long_long_max));


  unsigned long int addcl_carry_out_1;
  unsigned long int addcl_result_1 = __builtin_addcl(0UL, 0UL, 0UL, &addcl_carry_out_1);

  // __builtin_addcl(0UL, 0UL, 0UL, &addcl_carry_out_1) returns 0UL.
  __VERIFIER_assert(addcl_result_1 == 0UL);

  // __builtin_addcl(0UL, 0UL, 0UL, &addcl_carry_out_1) stores 0UL.
  __VERIFIER_assert(addcl_carry_out_1 == 0UL);


  unsigned long int addcl_carry_out_2;
  unsigned long int addcl_result_2 = __builtin_addcl(addcl_unsigned_long_max, 0UL, 0UL, &addcl_carry_out_2);

  // ILP32: __builtin_addcl(4294967295UL, 0UL, 0UL, &addcl_carry_out_2) returns 4294967295UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 0UL, 0UL, &addcl_carry_out_2) returns 18446744073709551615UL.
  __VERIFIER_assert(addcl_result_2 == addcl_unsigned_long_max);

  // ILP32: __builtin_addcl(4294967295UL, 0UL, 0UL, &addcl_carry_out_2) stores 0UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 0UL, 0UL, &addcl_carry_out_2) stores 0UL.
  __VERIFIER_assert(addcl_carry_out_2 == 0UL);


  unsigned long int addcl_carry_out_3;
  unsigned long int addcl_result_3 = __builtin_addcl(1UL, 2UL, 1UL, &addcl_carry_out_3);

  // __builtin_addcl(1UL, 2UL, 1UL, &addcl_carry_out_3) returns 4UL.
  __VERIFIER_assert(addcl_result_3 == 4UL);

  // __builtin_addcl(1UL, 2UL, 1UL, &addcl_carry_out_3) stores 0UL.
  __VERIFIER_assert(addcl_carry_out_3 == 0UL);


  unsigned long int addcl_carry_out_4;
  unsigned long int addcl_result_4 = __builtin_addcl(addcl_unsigned_long_max, 1UL, 0UL, &addcl_carry_out_4);

  // ILP32: __builtin_addcl(4294967295UL, 1UL, 0UL, &addcl_carry_out_4) returns 0UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 1UL, 0UL, &addcl_carry_out_4) returns 0UL.
  __VERIFIER_assert(addcl_result_4 == 0UL);

  // ILP32: __builtin_addcl(4294967295UL, 1UL, 0UL, &addcl_carry_out_4) stores 1UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 1UL, 0UL, &addcl_carry_out_4) stores 1UL.
  __VERIFIER_assert(addcl_carry_out_4 == 1UL);


  unsigned long int addcl_carry_out_5;
  unsigned long int addcl_result_5 = __builtin_addcl(addcl_unsigned_long_max, 0UL, 1UL, &addcl_carry_out_5);

  // ILP32: __builtin_addcl(4294967295UL, 0UL, 1UL, &addcl_carry_out_5) returns 0UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 0UL, 1UL, &addcl_carry_out_5) returns 0UL.
  __VERIFIER_assert(addcl_result_5 == 0UL);

  // ILP32: __builtin_addcl(4294967295UL, 0UL, 1UL, &addcl_carry_out_5) stores 1UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 0UL, 1UL, &addcl_carry_out_5) stores 1UL.
  __VERIFIER_assert(addcl_carry_out_5 == 1UL);


  unsigned long int addcl_carry_out_6;
  unsigned long int addcl_result_6 = __builtin_addcl(addcl_unsigned_long_max, addcl_unsigned_long_max, 2UL, &addcl_carry_out_6);

  // ILP32: __builtin_addcl(4294967295UL, 4294967295UL, 2UL, &addcl_carry_out_6) returns 0UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 18446744073709551615UL, 2UL, &addcl_carry_out_6) returns 0UL.
  __VERIFIER_assert(addcl_result_6 == 0UL);

  // ILP32: __builtin_addcl(4294967295UL, 4294967295UL, 2UL, &addcl_carry_out_6) stores 1UL.
  // LP64: __builtin_addcl(18446744073709551615UL, 18446744073709551615UL, 2UL, &addcl_carry_out_6) stores 1UL.
  __VERIFIER_assert(addcl_carry_out_6 == 1UL);


  unsigned long int addcl_carry_out_7;
  unsigned long int addcl_result_7 = __builtin_addcl(1UL, 0UL, addcl_unsigned_long_max, &addcl_carry_out_7);

  // ILP32: __builtin_addcl(1UL, 0UL, 4294967295UL, &addcl_carry_out_7) returns 0UL.
  // LP64: __builtin_addcl(1UL, 0UL, 18446744073709551615UL, &addcl_carry_out_7) returns 0UL.
  __VERIFIER_assert(addcl_result_7 == 0UL);

  // ILP32: __builtin_addcl(1UL, 0UL, 4294967295UL, &addcl_carry_out_7) stores 1UL.
  // LP64: __builtin_addcl(1UL, 0UL, 18446744073709551615UL, &addcl_carry_out_7) stores 1UL.
  __VERIFIER_assert(addcl_carry_out_7 == 1UL);


  // Tests for __builtin_addcll.

  unsigned long long int addcll_carry_out_1;
  unsigned long long int addcll_result_1 = __builtin_addcll(0ULL, 0ULL, 0ULL, &addcll_carry_out_1);

  // __builtin_addcll(0ULL, 0ULL, 0ULL, &addcll_carry_out_1) returns 0ULL.
  __VERIFIER_assert(addcll_result_1 == 0ULL);

  // __builtin_addcll(0ULL, 0ULL, 0ULL, &addcll_carry_out_1) stores 0ULL.
  __VERIFIER_assert(addcll_carry_out_1 == 0ULL);


  unsigned long long int addcll_carry_out_2;
  unsigned long long int addcll_result_2 = __builtin_addcll(unsigned_long_long_max, 0ULL, 0ULL, &addcll_carry_out_2);

  // __builtin_addcll(18446744073709551615ULL, 0ULL, 0ULL, &addcll_carry_out_2) returns 18446744073709551615ULL.
  __VERIFIER_assert(addcll_result_2 == unsigned_long_long_max);

  // __builtin_addcll(18446744073709551615ULL, 0ULL, 0ULL, &addcll_carry_out_2) stores 0ULL.
  __VERIFIER_assert(addcll_carry_out_2 == 0ULL);


  unsigned long long int addcll_carry_out_3;
  unsigned long long int addcll_result_3 = __builtin_addcll(1ULL, 2ULL, 1ULL, &addcll_carry_out_3);

  // __builtin_addcll(1ULL, 2ULL, 1ULL, &addcll_carry_out_3) returns 4ULL.
  __VERIFIER_assert(addcll_result_3 == 4ULL);

  // __builtin_addcll(1ULL, 2ULL, 1ULL, &addcll_carry_out_3) stores 0ULL.
  __VERIFIER_assert(addcll_carry_out_3 == 0ULL);


  unsigned long long int addcll_carry_out_4;
  unsigned long long int addcll_result_4 = __builtin_addcll(unsigned_long_long_max, 1ULL, 0ULL, &addcll_carry_out_4);

  // __builtin_addcll(18446744073709551615ULL, 1ULL, 0ULL, &addcll_carry_out_4) returns 0ULL.
  __VERIFIER_assert(addcll_result_4 == 0ULL);

  // __builtin_addcll(18446744073709551615ULL, 1ULL, 0ULL, &addcll_carry_out_4) stores 1ULL.
  __VERIFIER_assert(addcll_carry_out_4 == 1ULL);


  unsigned long long int addcll_carry_out_5;
  unsigned long long int addcll_result_5 = __builtin_addcll(unsigned_long_long_max, 0ULL, 1ULL, &addcll_carry_out_5);

  // __builtin_addcll(18446744073709551615ULL, 0ULL, 1ULL, &addcll_carry_out_5) returns 0ULL.
  __VERIFIER_assert(addcll_result_5 == 0ULL);

  // __builtin_addcll(18446744073709551615ULL, 0ULL, 1ULL, &addcll_carry_out_5) stores 1ULL.
  __VERIFIER_assert(addcll_carry_out_5 == 1ULL);


  unsigned long long int addcll_carry_out_6;
  unsigned long long int addcll_result_6 = __builtin_addcll(unsigned_long_long_max, unsigned_long_long_max, 2ULL, &addcll_carry_out_6);

  // __builtin_addcll(18446744073709551615ULL, 18446744073709551615ULL, 2ULL, &addcll_carry_out_6) returns 0ULL.
  __VERIFIER_assert(addcll_result_6 == 0ULL);

  // __builtin_addcll(18446744073709551615ULL, 18446744073709551615ULL, 2ULL, &addcll_carry_out_6) stores 1ULL.
  __VERIFIER_assert(addcll_carry_out_6 == 1ULL);


  unsigned long long int addcll_carry_out_7;
  unsigned long long int addcll_result_7 = __builtin_addcll(1ULL, 0ULL, unsigned_long_long_max, &addcll_carry_out_7);

  // __builtin_addcll(1ULL, 0ULL, 18446744073709551615ULL, &addcll_carry_out_7) returns 0ULL.
  __VERIFIER_assert(addcll_result_7 == 0ULL);

  // __builtin_addcll(1ULL, 0ULL, 18446744073709551615ULL, &addcll_carry_out_7) stores 1ULL.
  __VERIFIER_assert(addcll_carry_out_7 == 1ULL);
  return 0;
}
