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

union c_union {
  char first;
  char second;
};

union s_union {
  short first;
  short second;
};

union i_union {
  int first;
  int second;
};

union l_union {
  long first;
  long second;
};

union ll_union {
  long long first;
  long long second;
};


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// Only the first union member is initialized to 11 and examined; no inactive member is read.
int main(void) {

  int all_expected_checks_fail = 0;

  union c_union c = {11};
  union s_union s = {11};
  union i_union i = {11};
  union l_union l = {11};
  union ll_union ll = {11};

  all_expected_checks_fail = all_expected_checks_fail || (c.first != 11);
  all_expected_checks_fail = all_expected_checks_fail || (s.first != 11);
  all_expected_checks_fail = all_expected_checks_fail || (i.first != 11);
  all_expected_checks_fail = all_expected_checks_fail || (l.first != 11);
  all_expected_checks_fail = all_expected_checks_fail || (ll.first != 11);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
