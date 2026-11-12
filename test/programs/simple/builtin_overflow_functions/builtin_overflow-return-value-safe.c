// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Typical usage of the builtin overflow functions in the AWS C Common library:
// only the return value is used, the result is written to an unused local variable.

void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}

_Bool overflow_plus(unsigned long a, unsigned long b) {
  unsigned long c;
  return __builtin_uaddl_overflow(a, b, &c);
}

_Bool overflow_mult(unsigned long a, unsigned long b) {
  unsigned long c;
  return __builtin_umull_overflow(a, b, &c);
}

_Bool overflow_sub(int a, int b) {
  int c;
  return __builtin_sub_overflow(a, b, &c);
}

// ILP32 and LP64: safe for unreach-label, valid-memsafety, and no-overflow.
int main(void) {
  const unsigned long ulong_max = ~0UL;

  __VERIFIER_assert(!overflow_plus(1UL, 2UL));
  __VERIFIER_assert(overflow_plus(ulong_max, 1UL));
  __VERIFIER_assert(!overflow_mult(ulong_max, 1UL));
  __VERIFIER_assert(overflow_mult(ulong_max, 2UL));
  __VERIFIER_assert(!overflow_sub(-2147483647, 1));
  __VERIFIER_assert(overflow_sub(-2147483647 - 1, 1));

  // The _p variants have no side effect, their third argument only determines the type.
  __VERIFIER_assert(__builtin_add_overflow_p(127, 1, (signed char)0));
  __VERIFIER_assert(!__builtin_add_overflow_p(127, 1, (short)0));

  // Calls whose return value is ignored must be handled, too.
  int unused;
  __builtin_add_overflow(1, 2, &unused);
  return 0;
}
