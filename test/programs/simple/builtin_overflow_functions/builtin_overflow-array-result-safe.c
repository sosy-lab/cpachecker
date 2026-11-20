// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The result argument can also be an array, which decays to a pointer to its first element.

void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}

// ILP32 and LP64: safe for unreach-label, valid-memsafety, and no-overflow.
int main(void) {
  unsigned int result[2] = {5U, 7U};
  int overflow = __builtin_uadd_overflow(4294967295U, 2U, result);
  __VERIFIER_assert(overflow == 1);
  __VERIFIER_assert(result[0] == 1U);
  __VERIFIER_assert(result[1] == 7U);

  overflow = __builtin_mul_overflow(6, 7, result);
  __VERIFIER_assert(overflow == 0);
  __VERIFIER_assert(result[0] == 42U);
  return 0;
}
