// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}

// ILP32 and LP64: safe for unreach-label, valid-memsafety, and no-overflow.
int main(void) {
  long int long_result;
  int long_overflow = __builtin_saddl_overflow(2147483647L, 1L, &long_result);
  // The sum overflows 32-bit long, but fits 64-bit long.
  if (sizeof(long int) == 4U) {
    __VERIFIER_assert(long_result == (-2147483647L - 1L));
    __VERIFIER_assert(long_overflow == 1);
  } else {
    __VERIFIER_assert(long_result == (long int)2147483648LL);
    __VERIFIER_assert(long_overflow == 0);
  }

  typedef signed int result_type;
  result_type typedef_result;
  int typedef_overflow = __builtin_ssub_overflow(7, 3, &typedef_result);
  __VERIFIER_assert(typedef_result == 4);
  __VERIFIER_assert(typedef_overflow == 0);

  int operand = 6;
  int *pointer = &operand;
  int dereference_result;
  int dereference_overflow = __builtin_smul_overflow(*pointer, 7, &dereference_result);
  __VERIFIER_assert(dereference_result == 42);
  __VERIFIER_assert(dereference_overflow == 0);

  long long int wide_operand = 2147483648LL;
  int converted_result;
  int converted_overflow = __builtin_sadd_overflow(wide_operand, -100, &converted_result);
  // GCC converts the first operand to INT_MIN before addition; the builtin reports overflow.
  __VERIFIER_assert(converted_result == 2147483548);
  __VERIFIER_assert(converted_overflow == 1);
  return 0;
}
