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


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// GCC diagnoses the excess literal characters and keeps only the in-bounds prefix.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  // Subcase: gcc-string-truncation-one-original.
  {
    char a[1] = "xy";

    __VERIFIER_assert(sizeof a == 1U);
    __VERIFIER_assert(a[0] == 'x');
  }

  // Subcase: string-initializer-len2-size1.
  {
    char value[1] = "ab";

    __VERIFIER_assert(sizeof value == 1U);
    __VERIFIER_assert(value[0] == 'a');
  }

  return 0;
}
