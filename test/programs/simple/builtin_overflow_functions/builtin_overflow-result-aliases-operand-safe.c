// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);

void reach_error(void) {}
void __VERIFIER_error(void) {}

void __VERIFIER_assert(int cond) {
  if (!cond) {
    // Calls the error functions of both unreach-call variants (SV-COMP and CPAchecker tests).
    ERROR: { reach_error(); __VERIFIER_error(); abort(); }
  }
}

// Supported models: ILP32 and LP64.
// Expected verdicts: unreach-call true, valid-memsafety true, no-overflow true.
// The result pointer points to an operand. The operands are passed by value, so the returned
// overflow is determined by the operand values before the result is stored.
int main(void) {
  // 100 + 100 = 200 fits into unsigned char, although 200 + 100 would overflow.
  unsigned char x = 100;
  int overflow = __builtin_add_overflow(x, 100, &x);
  __VERIFIER_assert(overflow == 0);
  __VERIFIER_assert(x == 200);

  // 200 + 100 = 300 overflows to 44, although 44 + 100 would fit.
  unsigned char y = 200;
  overflow = __builtin_add_overflow(y, 100, &y);
  __VERIFIER_assert(overflow == 1);
  __VERIFIER_assert(y == 44);

  // The second operand is the result: 2U * 3000000000U overflows to 1705032704U, although
  // 2U * 1705032704U would fit.
  unsigned int n = 3000000000U;
  overflow = __builtin_mul_overflow(2U, n, &n);
  __VERIFIER_assert(overflow == 1);
  __VERIFIER_assert(n == 1705032704U);

  // Both operands are the result: 2000000000 + 2000000000 overflows to -294967296, although
  // -294967296 + -294967296 would fit.
  int s = 2000000000;
  overflow = __builtin_sadd_overflow(s, s, &s);
  __VERIFIER_assert(overflow == 1);
  __VERIFIER_assert(s == -294967296);

  return 0;
}
