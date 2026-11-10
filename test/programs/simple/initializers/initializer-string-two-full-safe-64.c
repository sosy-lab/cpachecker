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
// Every in-bounds element contains the literal prefix; there is no terminator in this array.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  // Subcase: string-two-full-original.
  {
    char a[2] = "xy";

    __VERIFIER_assert(sizeof a == 2U);
    __VERIFIER_assert(a[0] == 'x');
    __VERIFIER_assert(a[1] == 'y');
  }

  // Subcase: string-initializer-len2-size2.
  {
    char value[2] = "ab";

    __VERIFIER_assert(sizeof value == 2U);
    __VERIFIER_assert(value[0] == 'a');
    __VERIFIER_assert(value[1] == 'b');
  }

  return 0;
}
