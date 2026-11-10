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
// Each local untagged structure retains members 11 and 22; GCC discards the excess initializer 33.
int main(void) {

  int all_expected_checks_fail = 0;

  struct {
    char first;
    char second;
  } char_value = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (char_value.first != 11);
  all_expected_checks_fail = all_expected_checks_fail || (char_value.second != 22);

  struct {
    short first;
    short second;
  } short_value = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (short_value.first != 11);
  all_expected_checks_fail = all_expected_checks_fail || (short_value.second != 22);

  struct {
    int first;
    int second;
  } int_value = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (int_value.first != 11);
  all_expected_checks_fail = all_expected_checks_fail || (int_value.second != 22);

  struct {
    long first;
    long second;
  } long_value = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (long_value.first != 11);
  all_expected_checks_fail = all_expected_checks_fail || (long_value.second != 22);

  struct {
    long long first;
    long long second;
  } long_long_value = {11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (long_long_value.first != 11);
  all_expected_checks_fail = all_expected_checks_fail || (long_long_value.second != 22);

  // Only if every deliberately negated check fails does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
