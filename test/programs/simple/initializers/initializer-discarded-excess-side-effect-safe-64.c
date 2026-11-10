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
// GCC discards the excess increment expressions; every counter stays zero and each array is {11, 22}.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  int c_counter = 0;
  char c[2] = {11, 22, ++c_counter};
  int s_counter = 0;
  short s[2] = {11, 22, ++s_counter};
  int i_counter = 0;
  int i[2] = {11, 22, ++i_counter};
  int l_counter = 0;
  long l[2] = {11, 22, ++l_counter};
  int ll_counter = 0;
  long long ll[2] = {11, 22, ++ll_counter};

  __VERIFIER_assert(c[0] == 11);
  __VERIFIER_assert(c[1] == 22);
  __VERIFIER_assert(c_counter == 0);
  __VERIFIER_assert(s[0] == 11);
  __VERIFIER_assert(s[1] == 22);
  __VERIFIER_assert(s_counter == 0);
  __VERIFIER_assert(i[0] == 11);
  __VERIFIER_assert(i[1] == 22);
  __VERIFIER_assert(i_counter == 0);
  __VERIFIER_assert(l[0] == 11);
  __VERIFIER_assert(l[1] == 22);
  __VERIFIER_assert(l_counter == 0);
  __VERIFIER_assert(ll[0] == 11);
  __VERIFIER_assert(ll[1] == 22);
  __VERIFIER_assert(ll_counter == 0);

  return 0;
}
