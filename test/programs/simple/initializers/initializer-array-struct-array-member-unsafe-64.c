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

struct Point { long coords[2]; char *label; };


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// Each struct element initializes its array member; omitted array elements, members and the
// completely omitted third element are zero.
int main(void) {

  struct Point points[3] = {{{1, 2}, 0}, {{4}}};

  int all_expected_checks_fail = 0;

  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(long) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(void *) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(points[0].coords[0] == 1);
  all_expected_checks_fail = all_expected_checks_fail || !(points[0].coords[1] == 2);
  all_expected_checks_fail = all_expected_checks_fail || !(points[0].label == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(points[1].coords[0] == 4);
  all_expected_checks_fail = all_expected_checks_fail || !(points[1].coords[1] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(points[1].label == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(points[2].coords[0] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(points[2].coords[1] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(points[2].label == 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
