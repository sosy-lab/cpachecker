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
// GCC preserves each one-element array's size and value 11 while discarding both excess values 22 and 33.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char char_values[1] = {11, 22, 33};

  __VERIFIER_assert(sizeof char_values == 1U * sizeof(char));
  __VERIFIER_assert(char_values[0] == 11);

  short short_values[1] = {11, 22, 33};

  __VERIFIER_assert(sizeof short_values == 1U * sizeof(short));
  __VERIFIER_assert(short_values[0] == 11);

  int int_values[1] = {11, 22, 33};

  __VERIFIER_assert(sizeof int_values == 1U * sizeof(int));
  __VERIFIER_assert(int_values[0] == 11);

  long long_values[1] = {11, 22, 33};

  __VERIFIER_assert(sizeof long_values == 1U * sizeof(long));
  __VERIFIER_assert(long_values[0] == 11);

  long long long_long_values[1] = {11, 22, 33};

  __VERIFIER_assert(sizeof long_long_values == 1U * sizeof(long long));
  __VERIFIER_assert(long_long_values[0] == 11);

  return 0;
}
