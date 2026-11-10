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
// Each local untagged structure retains members 11 and 22; GCC discards the excess initializer 33.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  struct {
    char first;
    char second;
  } char_value = {11, 22, 33};

  __VERIFIER_assert(char_value.first == 11);
  __VERIFIER_assert(char_value.second == 22);

  struct {
    short first;
    short second;
  } short_value = {11, 22, 33};

  __VERIFIER_assert(short_value.first == 11);
  __VERIFIER_assert(short_value.second == 22);

  struct {
    int first;
    int second;
  } int_value = {11, 22, 33};

  __VERIFIER_assert(int_value.first == 11);
  __VERIFIER_assert(int_value.second == 22);

  struct {
    long first;
    long second;
  } long_value = {11, 22, 33};

  __VERIFIER_assert(long_value.first == 11);
  __VERIFIER_assert(long_value.second == 22);

  struct {
    long long first;
    long long second;
  } long_long_value = {11, 22, 33};

  __VERIFIER_assert(long_long_value.first == 11);
  __VERIFIER_assert(long_long_value.second == 22);

  return 0;
}
