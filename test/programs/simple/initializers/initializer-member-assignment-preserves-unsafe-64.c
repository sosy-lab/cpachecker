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

struct c_record {
  char first;
  char second;
};

struct s_record {
  short first;
  short second;
};

struct i_record {
  int first;
  int second;
};

struct l_record {
  long first;
  long second;
};

struct ll_record {
  long long first;
  long long second;
};


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// Changing each first member to 33 preserves its second member's initialized value 22.
int main(void) {

  int all_expected_checks_fail = 0;

  struct c_record c = {11, 22};
  c.first = 33;
  struct s_record s = {11, 22};
  s.first = 33;
  struct i_record i = {11, 22};
  i.first = 33;
  struct l_record l = {11, 22};
  l.first = 33;
  struct ll_record ll = {11, 22};
  ll.first = 33;

  all_expected_checks_fail = all_expected_checks_fail || (c.first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (c.second != 22);
  all_expected_checks_fail = all_expected_checks_fail || (s.first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (s.second != 22);
  all_expected_checks_fail = all_expected_checks_fail || (i.first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (i.second != 22);
  all_expected_checks_fail = all_expected_checks_fail || (l.first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (l.second != 22);
  all_expected_checks_fail = all_expected_checks_fail || (ll.first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (ll.second != 22);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
