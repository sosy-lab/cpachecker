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
// The inferred bound includes the literal terminator; all fourteen elements are initialized.
int main(void) {

  int all_expected_checks_fail = 0;

  char a[] = "abcdefghijklm";

  all_expected_checks_fail = all_expected_checks_fail || (sizeof a != 14UL);
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
  all_expected_checks_fail = all_expected_checks_fail || (a[11] != 'l');
  all_expected_checks_fail = all_expected_checks_fail || (a[12] != 'm');
  all_expected_checks_fail = all_expected_checks_fail || (a[13] != '\0');

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
