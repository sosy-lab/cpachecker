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
// GCC discards the excess increment expressions; every counter stays zero and each array is {11, 22}.
int main(void) {

  int all_expected_checks_fail = 0;

  int c_counter = 0;
  char c[2] = {11, 22, ++c_counter};
  int s_counter = 0;
  short s[2] = {11, 22, ++s_counter};
  int i_counter = 0;
  int i[2] = {11, 22, ++i_counter};
  int l_counter = 0;
  long l[2] = {11, 22, ++l_counter};
  int ll_counter = 0;
  long long ll[2] = {11, 22, ++ll_counter};

  all_expected_checks_fail = all_expected_checks_fail || (c[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (c[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (c_counter != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (s[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (s_counter != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (i[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (i_counter != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (l[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (l_counter != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (ll[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (ll_counter != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
