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

extern int snprintf(char *dest, unsigned long size, const char *format, ...);


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// Both bounded outputs reserve their last available byte for a terminator.
int main(void) {

  int all_expected_checks_fail = 0;

  char one[1] = {'Z'};
  char two[2] = {'Z', 'Z'};

  snprintf(one, 1UL, "%s", "x");
  snprintf(two, 2UL, "%s", "xy");

  all_expected_checks_fail = all_expected_checks_fail || (one[0] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (two[0] != 'x');
  all_expected_checks_fail = all_expected_checks_fail || (two[1] != '\0');

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
