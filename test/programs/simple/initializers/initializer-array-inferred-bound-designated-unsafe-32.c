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


// Expected: false (unsafe for unreach-label). Machine model: ILP32 (32).
// The designator determines the array length of five elements; elements between the explicit
// initializers are zero.
int main(void) {

  long values[] = {1, [4] = 5};

  int all_expected_checks_fail = 0;

  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(long) == 4);
  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(void *) == 4);
  all_expected_checks_fail = all_expected_checks_fail || !(sizeof values == 5 * sizeof(long));
  all_expected_checks_fail = all_expected_checks_fail || !(values[0] == 1);
  all_expected_checks_fail = all_expected_checks_fail || !(values[1] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(values[2] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(values[3] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(values[4] == 5);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
