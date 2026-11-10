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
// GCC preserves both declared sizes and in-bounds prefixes; neither array contains a terminator.
int main(void) {

  int all_expected_checks_fail = 0;

  // Subcase: gcc-string-truncation-original.
  {
    char a[5] = "blabla";

    all_expected_checks_fail = all_expected_checks_fail || (sizeof a != 5U);
    all_expected_checks_fail = all_expected_checks_fail || (a[0] != 'b');
    all_expected_checks_fail = all_expected_checks_fail || (a[1] != 'l');
    all_expected_checks_fail = all_expected_checks_fail || (a[2] != 'a');
    all_expected_checks_fail = all_expected_checks_fail || (a[3] != 'b');
    all_expected_checks_fail = all_expected_checks_fail || (a[4] != 'l');
  }

  // Subcase: gcc-string-truncation-long.
  {
    char a[11] = "abcdefghijklm";

    all_expected_checks_fail = all_expected_checks_fail || (sizeof a != 11U);
    all_expected_checks_fail = all_expected_checks_fail || (a[0] != 'a');
    all_expected_checks_fail = all_expected_checks_fail || (a[1] != 'b');
    all_expected_checks_fail = all_expected_checks_fail || (a[2] != 'c');
    all_expected_checks_fail = all_expected_checks_fail || (a[3] != 'd');
    all_expected_checks_fail = all_expected_checks_fail || (a[4] != 'e');
    all_expected_checks_fail = all_expected_checks_fail || (a[5] != 'f');
    all_expected_checks_fail = all_expected_checks_fail || (a[6] != 'g');
    all_expected_checks_fail = all_expected_checks_fail || (a[7] != 'h');
    all_expected_checks_fail = all_expected_checks_fail || (a[8] != 'i');
    all_expected_checks_fail = all_expected_checks_fail || (a[9] != 'j');
    all_expected_checks_fail = all_expected_checks_fail || (a[10] != 'k');
  }

  // Only if every deliberately negated check fails does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
