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

struct numeric_values {
  char c;
  short s;
  int i;
  long l;
  long long ll;
};

struct outer_record {
  int marker;
  struct numeric_values values;
};


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// The marker is 11; every member of the omitted nested structure is zero.
int main(void) {

  int all_expected_checks_fail = 0;

  struct outer_record object = {11};

  all_expected_checks_fail = all_expected_checks_fail || (object.marker != 11);
  all_expected_checks_fail = all_expected_checks_fail || (object.values.c != 0);
  all_expected_checks_fail = all_expected_checks_fail || (object.values.s != 0);
  all_expected_checks_fail = all_expected_checks_fail || (object.values.i != 0);
  all_expected_checks_fail = all_expected_checks_fail || (object.values.l != 0);
  all_expected_checks_fail = all_expected_checks_fail || (object.values.ll != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
