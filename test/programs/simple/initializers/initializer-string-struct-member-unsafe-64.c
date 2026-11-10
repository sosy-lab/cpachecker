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

struct Text { char text[19]; char marker; };


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// The text tail and omitted character member are zero-initialized.
int main(void) {

  int all_expected_checks_fail = 0;

  struct Text value = {"abcdefghijklm"};

  all_expected_checks_fail = all_expected_checks_fail || (value.text[0] != 'a');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[1] != 'b');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[2] != 'c');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[3] != 'd');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[4] != 'e');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[5] != 'f');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[6] != 'g');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[7] != 'h');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[8] != 'i');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[9] != 'j');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[10] != 'k');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[11] != 'l');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[12] != 'm');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[13] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[14] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[15] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[16] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[17] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (value.text[18] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (value.marker != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
