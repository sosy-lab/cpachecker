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
// Both string tails and the completely omitted third row are zero.
int main(void) {

  int all_expected_checks_fail = 0;

  char a[3][19] = {"abcdefghijklm", "xy"};

  all_expected_checks_fail = all_expected_checks_fail || (a[0][0] != 'a');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][1] != 'b');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][2] != 'c');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][3] != 'd');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][4] != 'e');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][5] != 'f');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][6] != 'g');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][7] != 'h');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][8] != 'i');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][9] != 'j');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][10] != 'k');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][11] != 'l');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][12] != 'm');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][13] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][14] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][15] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][16] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][17] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[0][18] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][0] != 'x');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][1] != 'y');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][2] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][3] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][4] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][5] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][6] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][7] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][8] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][9] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][10] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][11] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][12] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][13] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][14] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][15] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][16] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][17] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[1][18] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][0] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][1] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][2] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][3] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][4] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][5] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][6] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][7] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][8] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][9] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][10] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][11] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][12] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][13] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][14] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][15] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][16] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][17] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (a[2][18] != '\0');

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
