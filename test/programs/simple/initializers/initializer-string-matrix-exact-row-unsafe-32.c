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
// Each row holds its string with a zero-padded tail; the last row is filled exactly by its
// string, so no terminator is stored beyond the matrix.
int main(void) {

  char names[3][4] = {"ab", "cde", "fghi"};

  int all_expected_checks_fail = 0;

  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(long) == 4);
  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(void *) == 4);
  all_expected_checks_fail = all_expected_checks_fail || !(names[0][0] == 'a');
  all_expected_checks_fail = all_expected_checks_fail || !(names[0][1] == 'b');
  all_expected_checks_fail = all_expected_checks_fail || !(names[0][2] == '\0');
  all_expected_checks_fail = all_expected_checks_fail || !(names[0][3] == '\0');
  all_expected_checks_fail = all_expected_checks_fail || !(names[1][0] == 'c');
  all_expected_checks_fail = all_expected_checks_fail || !(names[1][1] == 'd');
  all_expected_checks_fail = all_expected_checks_fail || !(names[1][2] == 'e');
  all_expected_checks_fail = all_expected_checks_fail || !(names[1][3] == '\0');
  all_expected_checks_fail = all_expected_checks_fail || !(names[2][0] == 'f');
  all_expected_checks_fail = all_expected_checks_fail || !(names[2][1] == 'g');
  all_expected_checks_fail = all_expected_checks_fail || !(names[2][2] == 'h');
  all_expected_checks_fail = all_expected_checks_fail || !(names[2][3] == 'i');

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
