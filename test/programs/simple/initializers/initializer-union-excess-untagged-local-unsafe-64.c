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
// Each local untagged union initializes its first member to 11; GCC discards the excess initializer 22.
int main(void) {

  int all_expected_checks_fail = 0;

  union {
    char first;
    char second;
  } char_value = {11, 22};

  all_expected_checks_fail = all_expected_checks_fail || (char_value.first != 11);

  union {
    short first;
    short second;
  } short_value = {11, 22};

  all_expected_checks_fail = all_expected_checks_fail || (short_value.first != 11);

  union {
    int first;
    int second;
  } int_value = {11, 22};

  all_expected_checks_fail = all_expected_checks_fail || (int_value.first != 11);

  union {
    long first;
    long second;
  } long_value = {11, 22};

  all_expected_checks_fail = all_expected_checks_fail || (long_value.first != 11);

  union {
    long long first;
    long long second;
  } long_long_value = {11, 22};

  all_expected_checks_fail = all_expected_checks_fail || (long_long_value.first != 11);

  // Only if every deliberately negated check fails does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
