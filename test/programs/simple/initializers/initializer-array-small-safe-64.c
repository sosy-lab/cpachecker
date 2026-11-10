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
// The one-element array is zero; the two-element array contains 11 and an implicit zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char c_one[1] = {0};
  char c_two[2] = {11};
  short s_one[1] = {0};
  short s_two[2] = {11};
  int i_one[1] = {0};
  int i_two[2] = {11};
  long l_one[1] = {0};
  long l_two[2] = {11};
  long long ll_one[1] = {0};
  long long ll_two[2] = {11};

  __VERIFIER_assert(c_one[0] == 0);
  __VERIFIER_assert(c_two[0] == 11);
  __VERIFIER_assert(c_two[1] == 0);
  __VERIFIER_assert(s_one[0] == 0);
  __VERIFIER_assert(s_two[0] == 11);
  __VERIFIER_assert(s_two[1] == 0);
  __VERIFIER_assert(i_one[0] == 0);
  __VERIFIER_assert(i_two[0] == 11);
  __VERIFIER_assert(i_two[1] == 0);
  __VERIFIER_assert(l_one[0] == 0);
  __VERIFIER_assert(l_two[0] == 11);
  __VERIFIER_assert(l_two[1] == 0);
  __VERIFIER_assert(ll_one[0] == 0);
  __VERIFIER_assert(ll_two[0] == 11);
  __VERIFIER_assert(ll_two[1] == 0);

  return 0;
}
