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
// The literal initializes its prefix; every remaining array element is zero.
int main(void) {

  int all_expected_checks_fail = 0;

  // Subcase: string-two-terminated-original.
  {
    char a[2] = "x";

    all_expected_checks_fail = all_expected_checks_fail || (sizeof a != 2U);
    all_expected_checks_fail = all_expected_checks_fail || (a[0] != 'x');
    all_expected_checks_fail = all_expected_checks_fail || (a[1] != '\0');
  }

  // Subcase: string-initializer-len1-size2.
  {
    char value[2] = "a";

    all_expected_checks_fail = all_expected_checks_fail || (sizeof value != 2U);
    all_expected_checks_fail = all_expected_checks_fail || (value[0] != 'a');
    all_expected_checks_fail = all_expected_checks_fail || (value[1] != '\0');
  }

  // Only if every deliberately negated check fails does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
