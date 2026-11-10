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
// GCC diagnoses and discards 33; each unnamed array contains only 11 and 22.
int main(void) {

  int all_expected_checks_fail = 0;

  char *c = (char[2]){11, 22, 33};
  short *s = (short[2]){11, 22, 33};
  int *i = (int[2]){11, 22, 33};
  long *l = (long[2]){11, 22, 33};
  long long *ll = (long long[2]){11, 22, 33};

  all_expected_checks_fail = all_expected_checks_fail || (c[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (c[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (s[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (s[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (i[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (i[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (l[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (l[1] != 22);
  all_expected_checks_fail = all_expected_checks_fail || (ll[0] != 11);
  all_expected_checks_fail = all_expected_checks_fail || (ll[1] != 22);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
