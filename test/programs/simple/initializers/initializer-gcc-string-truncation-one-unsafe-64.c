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
// GCC diagnoses the excess literal characters and keeps only the in-bounds prefix.
int main(void) {

  int all_expected_checks_fail = 0;

  // Subcase: gcc-string-truncation-one-original.
  {
    char a[1] = "xy";

    all_expected_checks_fail = all_expected_checks_fail || (sizeof a != 1U);
    all_expected_checks_fail = all_expected_checks_fail || (a[0] != 'x');
  }

  // Subcase: string-initializer-len2-size1.
  {
    char value[1] = "ab";

    all_expected_checks_fail = all_expected_checks_fail || (sizeof value != 1U);
    all_expected_checks_fail = all_expected_checks_fail || (value[0] != 'a');
  }

  // Only if every deliberately negated check fails does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
