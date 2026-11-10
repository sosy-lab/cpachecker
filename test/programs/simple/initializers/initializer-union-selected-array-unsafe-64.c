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
  char values[3];
  char alternative;
};

union s_union {
  short values[3];
  short alternative;
};

union i_union {
  int values[3];
  int alternative;
};

union l_union {
  long values[3];
  long alternative;
};

union ll_union {
  long long values[3];
  long long alternative;
};


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// The selected array member contains 11 followed by two implicit zeros.
int main(void) {

  int all_expected_checks_fail = 0;

  union c_union c = {.values = {11}};
  union s_union s = {.values = {11}};
  union i_union i = {.values = {11}};
  union l_union l = {.values = {11}};
  union ll_union ll = {.values = {11}};

  all_expected_checks_fail = all_expected_checks_fail || (c.values[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (c.values[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c.values[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s.values[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (s.values[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s.values[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i.values[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (i.values[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i.values[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l.values[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (l.values[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l.values[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll.values[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (ll.values[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll.values[2] != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
