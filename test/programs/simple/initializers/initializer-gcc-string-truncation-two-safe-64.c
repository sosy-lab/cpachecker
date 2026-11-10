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

  // Subcase: gcc-string-truncation-two-original.
  {
    char a[2] = "xyz";

    __VERIFIER_assert(sizeof a == 2U);
    __VERIFIER_assert(a[0] == 'x');
    __VERIFIER_assert(a[1] == 'y');
  }

  // Subcase: string-initializer-len13-size2.
  {
    char value[2] = "abcdefghijklm";

    __VERIFIER_assert(sizeof value == 2U);
    __VERIFIER_assert(value[0] == 'a');
    __VERIFIER_assert(value[1] == 'b');
  }

  return 0;
}
