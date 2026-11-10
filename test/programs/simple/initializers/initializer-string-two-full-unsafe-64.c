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


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// Every in-bounds element contains the literal prefix; there is no terminator in this array.
int main(void) {

  int all_expected_checks_fail = 0;

  // Subcase: string-two-full-original.
  {
    char a[2] = "xy";

    all_expected_checks_fail = all_expected_checks_fail || (sizeof a != 2U);
    all_expected_checks_fail = all_expected_checks_fail || (a[0] != 'x');
    all_expected_checks_fail = all_expected_checks_fail || (a[1] != 'y');
  }

  // Subcase: string-initializer-len2-size2.
  {
    char value[2] = "ab";

    all_expected_checks_fail = all_expected_checks_fail || (sizeof value != 2U);
    all_expected_checks_fail = all_expected_checks_fail || (value[0] != 'a');
    all_expected_checks_fail = all_expected_checks_fail || (value[1] != 'b');
  }

  // Only if every deliberately negated check fails does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
