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
// GCC diagnoses and discards 22; each scalar is initialized to 11.
int main(void) {

  int all_expected_checks_fail = 0;

  char c = {11, 22};
  short s = {11, 22};
  int i = {11, 22};
  long l = {11, 22};
  long long ll = {11, 22};

  all_expected_checks_fail = all_expected_checks_fail || (c != 11);
  all_expected_checks_fail = all_expected_checks_fail || (s != 11);
  all_expected_checks_fail = all_expected_checks_fail || (i != 11);
  all_expected_checks_fail = all_expected_checks_fail || (l != 11);
  all_expected_checks_fail = all_expected_checks_fail || (ll != 11);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
