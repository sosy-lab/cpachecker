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
// Partially initialized rows and the entirely omitted final row receive zeros.
int main(void) {

  int all_expected_checks_fail = 0;

  char c[3][3] = {{11}, {22}};
  short s[3][3] = {{11}, {22}};
  int i[3][3] = {{11}, {22}};
  long l[3][3] = {{11}, {22}};
  long long ll[3][3] = {{11}, {22}};

  all_expected_checks_fail = all_expected_checks_fail || (c[0][0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (c[0][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c[0][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c[1][0] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (c[1][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c[1][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c[2][0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c[2][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c[2][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[0][0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (s[0][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[0][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[1][0] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (s[1][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[1][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[2][0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[2][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[2][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[0][0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (i[0][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[0][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[1][0] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (i[1][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[1][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[2][0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[2][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[2][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[0][0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (l[0][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[0][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[1][0] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (l[1][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[1][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[2][0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[2][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[2][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[0][0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (ll[0][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[0][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[1][0] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (ll[1][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[1][2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[2][0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[2][1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[2][2] != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
