// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


int main(void) {

  // Constant test values are initialized directly and never modified.
  const unsigned int uint_max = 4294967295U;
  const unsigned long long int ull_max = 18446744073709551615ULL;
  const unsigned long int addcl_max = ~0UL;


  // This program targets ILP32 and fails its expected verdict under LP64.

  // ILP32: 4294967295UL + 1UL = 4294967296; stored result = 0UL and carry-out = 1UL because the sum exceeds 4294967295UL.
  unsigned long int model_addcl_cout;
  (void)__builtin_addcl(4294967295UL, 1UL, 0UL, &model_addcl_cout);

  if (!(model_addcl_cout == 1UL))
    goto ERROR;

  // Unsigned int add-with-carry tests.

  unsigned int addc_0_0_cin0_cout;
  unsigned int addc_0_0_cin0_res;
  addc_0_0_cin0_res = __builtin_addc(0U, 0U, 0U, &addc_0_0_cin0_cout);

  // 0U + 0U + carry-in 0U = 0; result = 0U and carry-out = 0U because 0 <= 4294967295U.
  if (!(addc_0_0_cin0_res == 0U))
    goto ERROR;

  if (!(addc_0_0_cin0_cout == 0U))
    goto ERROR;


  unsigned int addc_max_0_cin0_cout;
  unsigned int addc_max_0_cin0_res;
  addc_max_0_cin0_res = __builtin_addc(uint_max, 0U, 0U, &addc_max_0_cin0_cout);

  // 4294967295U + 0U + carry-in 0U = 4294967295; result = 4294967295U and carry-out = 0U because 4294967295 <= 4294967295U.
  if (!(addc_max_0_cin0_res == uint_max))
    goto ERROR;

  if (!(addc_max_0_cin0_cout == 0U))
    goto ERROR;


  unsigned int addc_1_2_cin1_cout;
  unsigned int addc_1_2_cin1_res;
  addc_1_2_cin1_res = __builtin_addc(1U, 2U, 1U, &addc_1_2_cin1_cout);

  // 1U + 2U + carry-in 1U = 4; result = 4U and carry-out = 0U because 4 <= 4294967295U.
  if (!(addc_1_2_cin1_res == 4U))
    goto ERROR;

  if (!(addc_1_2_cin1_cout == 0U))
    goto ERROR;


  unsigned int addc_max_1_cin0_cout;
  unsigned int addc_max_1_cin0_res;
  addc_max_1_cin0_res = __builtin_addc(uint_max, 1U, 0U, &addc_max_1_cin0_cout);

  // 4294967295U + 1U + carry-in 0U = 4294967296; stored result = 0U and carry-out = 1U because 4294967296 > 4294967295U.
  if (!(addc_max_1_cin0_res == 0U))
    goto ERROR;

  if (!(addc_max_1_cin0_cout == 1U))
    goto ERROR;


  unsigned int addc_max_0_cin1_cout;
  unsigned int addc_max_0_cin1_res;
  addc_max_0_cin1_res = __builtin_addc(uint_max, 0U, 1U, &addc_max_0_cin1_cout);

  // 4294967295U + 0U + carry-in 1U = 4294967296; stored result = 0U and carry-out = 1U because 4294967296 > 4294967295U.
  if (!(addc_max_0_cin1_res == 0U))
    goto ERROR;

  if (!(addc_max_0_cin1_cout == 1U))
    goto ERROR;


  unsigned int addc_max_max_cin2_cout;
  unsigned int addc_max_max_cin2_res;
  addc_max_max_cin2_res = __builtin_addc(uint_max, uint_max, 2U, &addc_max_max_cin2_cout);

  // 4294967295U + 4294967295U + carry-in 2U = 8589934592; stored result = 0U and carry-out = 1U because 8589934592 > 4294967295U.
  if (!(addc_max_max_cin2_res == 0U))
    goto ERROR;

  if (!(addc_max_max_cin2_cout == 1U))
    goto ERROR;


  unsigned int addc_1_0_cinmax_cout;
  unsigned int addc_1_0_cinmax_res;
  addc_1_0_cinmax_res = __builtin_addc(1U, 0U, uint_max, &addc_1_0_cinmax_cout);

  // 1U + 0U + carry-in 4294967295U = 4294967296; stored result = 0U and carry-out = 1U because 4294967296 > 4294967295U.
  if (!(addc_1_0_cinmax_res == 0U))
    goto ERROR;

  if (!(addc_1_0_cinmax_cout == 1U))
    goto ERROR;
  unsigned long int addcl_0_0_cin0_cout;
  unsigned long int addcl_0_0_cin0_res;
  addcl_0_0_cin0_res = __builtin_addcl(0UL, 0UL, 0UL, &addcl_0_0_cin0_cout);

  // 0UL + 0UL + carry-in 0UL = 0; result = 0UL and carry-out = 0UL because 0 <= 4294967295UL.
  if (!(addcl_0_0_cin0_res == 0UL))
    goto ERROR;

  if (!(addcl_0_0_cin0_cout == 0UL))
    goto ERROR;


  unsigned long int addcl_max_0_cin0_cout;
  unsigned long int addcl_max_0_cin0_res;
  addcl_max_0_cin0_res = __builtin_addcl(addcl_max, 0UL, 0UL, &addcl_max_0_cin0_cout);

  // 4294967295UL + 0UL + carry-in 0UL = 4294967295; result = 4294967295UL and carry-out = 0UL because 4294967295 <= 4294967295UL.
  if (!(addcl_max_0_cin0_res == addcl_max))
    goto ERROR;

  if (!(addcl_max_0_cin0_cout == 0UL))
    goto ERROR;


  unsigned long int addcl_1_2_cin1_cout;
  unsigned long int addcl_1_2_cin1_res;
  addcl_1_2_cin1_res = __builtin_addcl(1UL, 2UL, 1UL, &addcl_1_2_cin1_cout);

  // 1UL + 2UL + carry-in 1UL = 4; result = 4UL and carry-out = 0UL because 4 <= 4294967295UL.
  if (!(addcl_1_2_cin1_res == 4UL))
    goto ERROR;

  if (!(addcl_1_2_cin1_cout == 0UL))
    goto ERROR;


  unsigned long int addcl_max_1_cin0_cout;
  unsigned long int addcl_max_1_cin0_res;
  addcl_max_1_cin0_res = __builtin_addcl(addcl_max, 1UL, 0UL, &addcl_max_1_cin0_cout);

  // 4294967295UL + 1UL + carry-in 0UL = 4294967296; stored result = 0UL and carry-out = 1UL because 4294967296 > 4294967295UL.
  if (!(addcl_max_1_cin0_res == 0UL))
    goto ERROR;

  if (!(addcl_max_1_cin0_cout == 1UL))
    goto ERROR;


  unsigned long int addcl_max_0_cin1_cout;
  unsigned long int addcl_max_0_cin1_res;
  addcl_max_0_cin1_res = __builtin_addcl(addcl_max, 0UL, 1UL, &addcl_max_0_cin1_cout);

  // 4294967295UL + 0UL + carry-in 1UL = 4294967296; stored result = 0UL and carry-out = 1UL because 4294967296 > 4294967295UL.
  if (!(addcl_max_0_cin1_res == 0UL))
    goto ERROR;

  if (!(addcl_max_0_cin1_cout == 1UL))
    goto ERROR;


  unsigned long int addcl_max_max_cin2_cout;
  unsigned long int addcl_max_max_cin2_res;
  addcl_max_max_cin2_res = __builtin_addcl(addcl_max, addcl_max, 2UL, &addcl_max_max_cin2_cout);

  // 4294967295UL + 4294967295UL + carry-in 2UL = 8589934592; stored result = 0UL and carry-out = 1UL because 8589934592 > 4294967295UL.
  if (!(addcl_max_max_cin2_res == 0UL))
    goto ERROR;

  if (!(addcl_max_max_cin2_cout == 1UL))
    goto ERROR;


  unsigned long int addcl_1_0_cinmax_cout;
  unsigned long int addcl_1_0_cinmax_res;
  addcl_1_0_cinmax_res = __builtin_addcl(1UL, 0UL, addcl_max, &addcl_1_0_cinmax_cout);

  // 1UL + 0UL + carry-in 4294967295UL = 4294967296; stored result = 0UL and carry-out = 1UL because 4294967296 > 4294967295UL.
  if (!(addcl_1_0_cinmax_res == 0UL))
    goto ERROR;

  if (!(addcl_1_0_cinmax_cout == 1UL))
    goto ERROR;


  // Unsigned long long int add-with-carry tests.

  unsigned long long int addcll_0_0_cin0_cout;
  unsigned long long int addcll_0_0_cin0_res;
  addcll_0_0_cin0_res = __builtin_addcll(0ULL, 0ULL, 0ULL, &addcll_0_0_cin0_cout);

  // 0ULL + 0ULL + carry-in 0ULL = 0; result = 0ULL and carry-out = 0ULL because 0 <= 18446744073709551615ULL.
  if (!(addcll_0_0_cin0_res == 0ULL))
    goto ERROR;

  if (!(addcll_0_0_cin0_cout == 0ULL))
    goto ERROR;


  unsigned long long int addcll_max_0_cin0_cout;
  unsigned long long int addcll_max_0_cin0_res;
  addcll_max_0_cin0_res = __builtin_addcll(ull_max, 0ULL, 0ULL, &addcll_max_0_cin0_cout);

  // 18446744073709551615ULL + 0ULL + carry-in 0ULL = 18446744073709551615; result = 18446744073709551615ULL and carry-out = 0ULL because
  // 18446744073709551615 <= 18446744073709551615ULL.
  if (!(addcll_max_0_cin0_res == ull_max))
    goto ERROR;

  if (!(addcll_max_0_cin0_cout == 0ULL))
    goto ERROR;


  unsigned long long int addcll_1_2_cin1_cout;
  unsigned long long int addcll_1_2_cin1_res;
  addcll_1_2_cin1_res = __builtin_addcll(1ULL, 2ULL, 1ULL, &addcll_1_2_cin1_cout);

  // 1ULL + 2ULL + carry-in 1ULL = 4; result = 4ULL and carry-out = 0ULL because 4 <= 18446744073709551615ULL.
  if (!(addcll_1_2_cin1_res == 4ULL))
    goto ERROR;

  if (!(addcll_1_2_cin1_cout == 0ULL))
    goto ERROR;


  unsigned long long int addcll_max_1_cin0_cout;
  unsigned long long int addcll_max_1_cin0_res;
  addcll_max_1_cin0_res = __builtin_addcll(ull_max, 1ULL, 0ULL, &addcll_max_1_cin0_cout);

  // 18446744073709551615ULL + 1ULL + carry-in 0ULL = 18446744073709551616; stored result = 0ULL and carry-out = 1ULL because
  // 18446744073709551616 > 18446744073709551615ULL.
  if (!(addcll_max_1_cin0_res == 0ULL))
    goto ERROR;

  if (!(addcll_max_1_cin0_cout == 1ULL))
    goto ERROR;


  unsigned long long int addcll_max_0_cin1_cout;
  unsigned long long int addcll_max_0_cin1_res;
  addcll_max_0_cin1_res = __builtin_addcll(ull_max, 0ULL, 1ULL, &addcll_max_0_cin1_cout);

  // 18446744073709551615ULL + 0ULL + carry-in 1ULL = 18446744073709551616; stored result = 0ULL and carry-out = 1ULL because
  // 18446744073709551616 > 18446744073709551615ULL.
  if (!(addcll_max_0_cin1_res == 0ULL))
    goto ERROR;

  if (!(addcll_max_0_cin1_cout == 1ULL))
    goto ERROR;


  unsigned long long int addcll_max_max_cin2_cout;
  unsigned long long int addcll_max_max_cin2_res;
  addcll_max_max_cin2_res = __builtin_addcll(ull_max, ull_max, 2ULL, &addcll_max_max_cin2_cout);

  // 18446744073709551615ULL + 18446744073709551615ULL + carry-in 2ULL = 36893488147419103232; stored result = 0ULL and carry-out = 1ULL
  // because 36893488147419103232 > 18446744073709551615ULL.
  if (!(addcll_max_max_cin2_res == 0ULL))
    goto ERROR;

  if (!(addcll_max_max_cin2_cout == 1ULL))
    goto ERROR;


  unsigned long long int addcll_1_0_cinmax_cout;
  unsigned long long int addcll_1_0_cinmax_res;
  addcll_1_0_cinmax_res = __builtin_addcll(1ULL, 0ULL, ull_max, &addcll_1_0_cinmax_cout);

  // 1ULL + 0ULL + carry-in 18446744073709551615ULL = 18446744073709551616; stored result = 0ULL and carry-out = 1ULL because
  // 18446744073709551616 > 18446744073709551615ULL.
  if (!(addcll_1_0_cinmax_res == 0ULL))
    goto ERROR;

  if (!(addcll_1_0_cinmax_cout == 1ULL))
    goto ERROR;


  return 0;

ERROR:
  return 1;
}
