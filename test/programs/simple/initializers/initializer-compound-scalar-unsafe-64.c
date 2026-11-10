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
// Each scalar compound literal has the explicitly supplied value zero.
int main(void) {

  int all_expected_checks_fail = 0;

  char c = (char){0};
  short s = (short){0};
  int i = (int){0};
  long l = (long){0};
  long long ll = (long long){0};

  all_expected_checks_fail = all_expected_checks_fail || (c != 0);
  all_expected_checks_fail = all_expected_checks_fail || (s != 0);
  all_expected_checks_fail = all_expected_checks_fail || (i != 0);
  all_expected_checks_fail = all_expected_checks_fail || (l != 0);
  all_expected_checks_fail = all_expected_checks_fail || (ll != 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
