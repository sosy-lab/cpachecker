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

union Overlay { long longs[2]; int ints[4]; };


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// The long array of the first member overlays the int array (little endian); which ints hold
// the values depends on the size of long, the remaining bytes of the union are zero.
int main(void) {

  union Overlay value = {{1, 2}};

  int all_expected_checks_fail = 0;

  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(long) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(void *) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(value.longs[0] == 1);
  all_expected_checks_fail = all_expected_checks_fail || !(value.longs[1] == 2);
  all_expected_checks_fail = all_expected_checks_fail || !(value.ints[0] == 1);
  all_expected_checks_fail = all_expected_checks_fail || !(value.ints[1] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(value.ints[2] == 2);
  all_expected_checks_fail = all_expected_checks_fail || !(value.ints[3] == 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
