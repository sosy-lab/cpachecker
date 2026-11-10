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
// The first two elements are 11 and 22; every omitted element is zero.
int main(void) {

  int all_expected_checks_fail = 0;

  char c[5] = {11, 22};
  short s[5] = {11, 22};
  int i[5] = {11, 22};
  long l[5] = {11, 22};
  long long ll[5] = {11, 22};

  all_expected_checks_fail = all_expected_checks_fail || (c[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (c[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (c[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c[3] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c[4] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (s[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (s[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[3] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[4] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (i[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (i[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[3] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[4] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (l[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (l[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[3] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[4] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (ll[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (ll[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[3] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[4] != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
