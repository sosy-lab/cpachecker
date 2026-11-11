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

struct Text { long before; char text[4]; char after; };


// Expected: false (unsafe for unreach-label). Machine model: ILP32 (32).
// The string literal exactly fills the member array, so no terminator is stored; the member
// after the array, which is initialized first by its designator, keeps its value.
int main(void) {

  struct Text value = {.after = 'y', .before = -1, .text = "abcd"};

  int all_expected_checks_fail = 0;

  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(long) == 4);
  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(void *) == 4);
  all_expected_checks_fail = all_expected_checks_fail || !(value.before == -1);
  all_expected_checks_fail = all_expected_checks_fail || !(value.text[0] == 'a');
  all_expected_checks_fail = all_expected_checks_fail || !(value.text[1] == 'b');
  all_expected_checks_fail = all_expected_checks_fail || !(value.text[2] == 'c');
  all_expected_checks_fail = all_expected_checks_fail || !(value.text[3] == 'd');
  all_expected_checks_fail = all_expected_checks_fail || !(value.after == 'y');

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
