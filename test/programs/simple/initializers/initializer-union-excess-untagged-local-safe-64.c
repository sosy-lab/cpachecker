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
// Each local untagged union initializes its first member to 11; GCC discards the excess initializer 22.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  union {
    char first;
    char second;
  } char_value = {11, 22};

  __VERIFIER_assert(char_value.first == 11);

  union {
    short first;
    short second;
  } short_value = {11, 22};

  __VERIFIER_assert(short_value.first == 11);

  union {
    int first;
    int second;
  } int_value = {11, 22};

  __VERIFIER_assert(int_value.first == 11);

  union {
    long first;
    long second;
  } long_value = {11, 22};

  __VERIFIER_assert(long_value.first == 11);

  union {
    long long first;
    long long second;
  } long_long_value = {11, 22};

  __VERIFIER_assert(long_long_value.first == 11);

  return 0;
}
