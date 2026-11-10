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
// GCC retains each two-element array's size and values 11 and 22; excess 33 is discarded.
int main(void) {

  int all_expected_checks_fail = 0;

  char c[2] = {11, 22, 33};
  short s[2] = {11, 22, 33};
  int i[2] = {11, 22, 33};
  long l[2] = {11, 22, 33};
  long long ll[2] = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (sizeof c != 2U * sizeof(char));
  all_expected_checks_fail = all_expected_checks_fail || (sizeof s != 2U * sizeof(short));
  all_expected_checks_fail = all_expected_checks_fail || (sizeof i != 2U * sizeof(int));
  all_expected_checks_fail = all_expected_checks_fail || (sizeof l != 2U * sizeof(long));
  all_expected_checks_fail = all_expected_checks_fail || (sizeof ll != 2U * sizeof(long long));
  all_expected_checks_fail = all_expected_checks_fail || (c[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (c[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (s[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (s[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (i[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (i[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (l[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (l[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (ll[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (ll[1] != 22);

  // Only if every deliberately negated check fails does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
