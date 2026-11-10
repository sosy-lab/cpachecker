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
// The one-element array is zero; the two-element array contains 11 and an implicit zero.
int main(void) {

  int all_expected_checks_fail = 0;

  char c_one[1] = {0};
  char c_two[2] = {11};
  short s_one[1] = {0};
  short s_two[2] = {11};
  int i_one[1] = {0};
  int i_two[2] = {11};
  long l_one[1] = {0};
  long l_two[2] = {11};
  long long ll_one[1] = {0};
  long long ll_two[2] = {11};

  all_expected_checks_fail = all_expected_checks_fail || (c_one[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c_two[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (c_two[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s_one[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s_two[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (s_two[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i_one[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i_two[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (i_two[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l_one[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l_two[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (l_two[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll_one[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll_two[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (ll_two[1] != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
