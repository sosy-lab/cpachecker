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
// The explicit fourteen-element bound fits all thirteen characters and their terminator exactly.
int main(void) {

  int all_expected_checks_fail = 0;

  char value[14] = "abcdefghijklm";

  all_expected_checks_fail = all_expected_checks_fail || (sizeof value != 14U);
  all_expected_checks_fail = all_expected_checks_fail || (value[0] != 'a');
  all_expected_checks_fail = all_expected_checks_fail || (value[1] != 'b');
  all_expected_checks_fail = all_expected_checks_fail || (value[2] != 'c');
  all_expected_checks_fail = all_expected_checks_fail || (value[3] != 'd');
  all_expected_checks_fail = all_expected_checks_fail || (value[4] != 'e');
  all_expected_checks_fail = all_expected_checks_fail || (value[5] != 'f');
  all_expected_checks_fail = all_expected_checks_fail || (value[6] != 'g');
  all_expected_checks_fail = all_expected_checks_fail || (value[7] != 'h');
  all_expected_checks_fail = all_expected_checks_fail || (value[8] != 'i');
  all_expected_checks_fail = all_expected_checks_fail || (value[9] != 'j');
  all_expected_checks_fail = all_expected_checks_fail || (value[10] != 'k');
  all_expected_checks_fail = all_expected_checks_fail || (value[11] != 'l');
  all_expected_checks_fail = all_expected_checks_fail || (value[12] != 'm');
  all_expected_checks_fail = all_expected_checks_fail || (value[13] != '\0');

  // Only if every deliberately negated check fails does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
