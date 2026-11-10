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

extern char *strcpy(char *dest, const char *src);


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// The empty string fits in one byte; x and its terminator fit in two bytes.
int main(void) {

  int all_expected_checks_fail = 0;

  char one[1] = {'Z'};
  char two[2] = {'Z', 'Z'};

  strcpy(one, "");
  strcpy(two, "x");

  all_expected_checks_fail = all_expected_checks_fail || (one[0] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (two[0] != 'x');
  all_expected_checks_fail = all_expected_checks_fail || (two[1] != '\0');

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
