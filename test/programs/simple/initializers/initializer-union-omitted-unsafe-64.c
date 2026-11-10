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

struct union_container {
  int marker;
  union c_union c;
  union s_union s;
  union i_union i;
  union l_union l;
  union ll_union ll;
};


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// The marker is 11; each omitted union initializes its first named member to zero.
int main(void) {

  int all_expected_checks_fail = 0;

  struct union_container object = {11};

  all_expected_checks_fail = all_expected_checks_fail || (object.marker != 11);
  all_expected_checks_fail = all_expected_checks_fail || (object.c.first != 0);
  all_expected_checks_fail = all_expected_checks_fail || (object.s.first != 0);
  all_expected_checks_fail = all_expected_checks_fail || (object.i.first != 0);
  all_expected_checks_fail = all_expected_checks_fail || (object.l.first != 0);
  all_expected_checks_fail = all_expected_checks_fail || (object.ll.first != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
