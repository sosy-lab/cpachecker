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
// GCC preserves each one-element array's size and value 11 while discarding both excess values 22 and 33.
int main(void) {

  int all_expected_checks_fail = 0;

  char char_values[1] = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (sizeof char_values != 1U * sizeof(char));
  all_expected_checks_fail = all_expected_checks_fail || (char_values[0] != 11);

  short short_values[1] = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (sizeof short_values != 1U * sizeof(short));
  all_expected_checks_fail = all_expected_checks_fail || (short_values[0] != 11);

  int int_values[1] = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (sizeof int_values != 1U * sizeof(int));
  all_expected_checks_fail = all_expected_checks_fail || (int_values[0] != 11);

  long long_values[1] = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (sizeof long_values != 1U * sizeof(long));
  all_expected_checks_fail = all_expected_checks_fail || (long_values[0] != 11);

  long long long_long_values[1] = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (sizeof long_long_values != 1U * sizeof(long long));
  all_expected_checks_fail = all_expected_checks_fail || (long_long_values[0] != 11);

  // Only if every deliberately negated check fails does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
