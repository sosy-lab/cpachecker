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
// The first pointer retains its supplied address; omitted pointer elements are null pointers.
int main(void) {

  int all_expected_checks_fail = 0;

  char c_value = 11;
  char *c[3] = {&c_value};
  short s_value = 11;
  short *s[3] = {&s_value};
  int i_value = 11;
  int *i[3] = {&i_value};
  long l_value = 11;
  long *l[3] = {&l_value};
  long long ll_value = 11;
  long long *ll[3] = {&ll_value};

  all_expected_checks_fail = all_expected_checks_fail || (c[0] != &c_value);
  all_expected_checks_fail = all_expected_checks_fail || (c[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[0] != &s_value);
  all_expected_checks_fail = all_expected_checks_fail || (s[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[0] != &i_value);
  all_expected_checks_fail = all_expected_checks_fail || (i[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[0] != &l_value);
  all_expected_checks_fail = all_expected_checks_fail || (l[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[0] != &ll_value);
  all_expected_checks_fail = all_expected_checks_fail || (ll[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll[2] != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
