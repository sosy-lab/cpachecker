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


// Expected: true (safe for unreach-label). Machine model: ILP32 (32).
// The designator determines the array length of five elements; elements between the explicit
// initializers are zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  long values[] = {1, [4] = 5};

  __VERIFIER_assert(sizeof(long) == 4);
  __VERIFIER_assert(sizeof(void *) == 4);
  __VERIFIER_assert(sizeof values == 5 * sizeof(long));
  __VERIFIER_assert(values[0] == 1);
  __VERIFIER_assert(values[1] == 0);
  __VERIFIER_assert(values[2] == 0);
  __VERIFIER_assert(values[3] == 0);
  __VERIFIER_assert(values[4] == 5);

  return 0;
}
