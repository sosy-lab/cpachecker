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

struct Nested { char small; long big; };
struct Global { int scalar; struct Nested nested; short values[4]; char *pointer; };

struct Global global = {1, {2, 3}, {4, 5}};


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// The global struct initializes its nested struct and its array member; the array tail and the
// omitted pointer member are zero.
int main(void) {

  int all_expected_checks_fail = 0;

  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(long) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(sizeof(void *) == 8);
  all_expected_checks_fail = all_expected_checks_fail || !(global.scalar == 1);
  all_expected_checks_fail = all_expected_checks_fail || !(global.nested.small == 2);
  all_expected_checks_fail = all_expected_checks_fail || !(global.nested.big == 3);
  all_expected_checks_fail = all_expected_checks_fail || !(global.values[0] == 4);
  all_expected_checks_fail = all_expected_checks_fail || !(global.values[1] == 5);
  all_expected_checks_fail = all_expected_checks_fail || !(global.values[2] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(global.values[3] == 0);
  all_expected_checks_fail = all_expected_checks_fail || !(global.pointer == 0);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
