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
  char middle;
  char last;
};

struct s_record {
  short first;
  short middle;
  short last;
};

struct i_record {
  int first;
  int middle;
  int last;
};

struct l_record {
  long first;
  long middle;
  long last;
};

struct ll_record {
  long long first;
  long long middle;
  long long last;
};


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// The designated middle member is 22; both omitted surrounding members are zero.
int main(void) {

  int all_expected_checks_fail = 0;

  struct c_record c = {.middle = 22};
  struct s_record s = {.middle = 22};
  struct i_record i = {.middle = 22};
  struct l_record l = {.middle = 22};
  struct ll_record ll = {.middle = 22};

  all_expected_checks_fail = all_expected_checks_fail || (c.first != 0);
  all_expected_checks_fail = all_expected_checks_fail || (c.middle != 22);
  all_expected_checks_fail = all_expected_checks_fail || (c.last != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s.first != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s.middle != 22);
  all_expected_checks_fail = all_expected_checks_fail || (s.last != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i.first != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i.middle != 22);
  all_expected_checks_fail = all_expected_checks_fail || (i.last != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l.first != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l.middle != 22);
  all_expected_checks_fail = all_expected_checks_fail || (l.last != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll.first != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll.middle != 22);
  all_expected_checks_fail = all_expected_checks_fail || (ll.last != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
