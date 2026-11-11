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
// The pointer is initialized with the address of the string literal, which holds all
// characters including its terminator.
int main(void) {

  const char *pointer = "abc";

  int all_expected_checks_fail = 0;

  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(long) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(void *) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(pointer[0] == 'a');
  all_expected_checks_fail = all_expected_checks_fail || !(pointer[1] == 'b');
  all_expected_checks_fail = all_expected_checks_fail || !(pointer[2] == 'c');
  all_expected_checks_fail = all_expected_checks_fail || !(pointer[3] == '\0');

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
