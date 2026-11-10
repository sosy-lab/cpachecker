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


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// Both string tails and the completely omitted third row are zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char a[3][19] = {"abcdefghijklm", "xy"};

  __VERIFIER_assert(a[0][0] == 'a');
  __VERIFIER_assert(a[0][1] == 'b');
  __VERIFIER_assert(a[0][2] == 'c');
  __VERIFIER_assert(a[0][3] == 'd');
  __VERIFIER_assert(a[0][4] == 'e');
  __VERIFIER_assert(a[0][5] == 'f');
  __VERIFIER_assert(a[0][6] == 'g');
  __VERIFIER_assert(a[0][7] == 'h');
  __VERIFIER_assert(a[0][8] == 'i');
  __VERIFIER_assert(a[0][9] == 'j');
  __VERIFIER_assert(a[0][10] == 'k');
  __VERIFIER_assert(a[0][11] == 'l');
  __VERIFIER_assert(a[0][12] == 'm');
  __VERIFIER_assert(a[0][13] == '\0');
  __VERIFIER_assert(a[0][14] == '\0');
  __VERIFIER_assert(a[0][15] == '\0');
  __VERIFIER_assert(a[0][16] == '\0');
  __VERIFIER_assert(a[0][17] == '\0');
  __VERIFIER_assert(a[0][18] == '\0');
  __VERIFIER_assert(a[1][0] == 'x');
  __VERIFIER_assert(a[1][1] == 'y');
  __VERIFIER_assert(a[1][2] == '\0');
  __VERIFIER_assert(a[1][3] == '\0');
  __VERIFIER_assert(a[1][4] == '\0');
  __VERIFIER_assert(a[1][5] == '\0');
  __VERIFIER_assert(a[1][6] == '\0');
  __VERIFIER_assert(a[1][7] == '\0');
  __VERIFIER_assert(a[1][8] == '\0');
  __VERIFIER_assert(a[1][9] == '\0');
  __VERIFIER_assert(a[1][10] == '\0');
  __VERIFIER_assert(a[1][11] == '\0');
  __VERIFIER_assert(a[1][12] == '\0');
  __VERIFIER_assert(a[1][13] == '\0');
  __VERIFIER_assert(a[1][14] == '\0');
  __VERIFIER_assert(a[1][15] == '\0');
  __VERIFIER_assert(a[1][16] == '\0');
  __VERIFIER_assert(a[1][17] == '\0');
  __VERIFIER_assert(a[1][18] == '\0');
  __VERIFIER_assert(a[2][0] == '\0');
  __VERIFIER_assert(a[2][1] == '\0');
  __VERIFIER_assert(a[2][2] == '\0');
  __VERIFIER_assert(a[2][3] == '\0');
  __VERIFIER_assert(a[2][4] == '\0');
  __VERIFIER_assert(a[2][5] == '\0');
  __VERIFIER_assert(a[2][6] == '\0');
  __VERIFIER_assert(a[2][7] == '\0');
  __VERIFIER_assert(a[2][8] == '\0');
  __VERIFIER_assert(a[2][9] == '\0');
  __VERIFIER_assert(a[2][10] == '\0');
  __VERIFIER_assert(a[2][11] == '\0');
  __VERIFIER_assert(a[2][12] == '\0');
  __VERIFIER_assert(a[2][13] == '\0');
  __VERIFIER_assert(a[2][14] == '\0');
  __VERIFIER_assert(a[2][15] == '\0');
  __VERIFIER_assert(a[2][16] == '\0');
  __VERIFIER_assert(a[2][17] == '\0');
  __VERIFIER_assert(a[2][18] == '\0');

  return 0;
}
