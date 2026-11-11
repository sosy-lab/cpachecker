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

struct Inner { long values[3]; };
struct Outer { char first; long values[3]; struct Inner inner; int last; };


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// The array member and the array in the nested struct are initialized element-wise, with a
// designator in the nested one; omitted elements are zero and the following member is kept.
int main(void) {

  struct Outer value = {1, {2, 3}, {{4, [2] = 6}}, 7};

  int all_expected_checks_fail = 0;

  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(long) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(void *) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(value.first == 1);
  all_expected_checks_fail = all_expected_checks_fail || !(value.values[0] == 2);
  all_expected_checks_fail = all_expected_checks_fail || !(value.values[1] == 3);
  all_expected_checks_fail = all_expected_checks_fail || !(value.values[2] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(value.inner.values[0] == 4);
  all_expected_checks_fail = all_expected_checks_fail || !(value.inner.values[1] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(value.inner.values[2] == 6);
  all_expected_checks_fail = all_expected_checks_fail || !(value.last == 7);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
