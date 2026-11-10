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
// The first two elements are 11 and 22; every omitted element is zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char c[5] = {11, 22};
  short s[5] = {11, 22};
  int i[5] = {11, 22};
  long l[5] = {11, 22};
  long long ll[5] = {11, 22};

  __VERIFIER_assert(c[0] == 11);
  __VERIFIER_assert(c[1] == 22);
  __VERIFIER_assert(c[2] == 0);
  __VERIFIER_assert(c[3] == 0);
  __VERIFIER_assert(c[4] == 0);
  __VERIFIER_assert(s[0] == 11);
  __VERIFIER_assert(s[1] == 22);
  __VERIFIER_assert(s[2] == 0);
  __VERIFIER_assert(s[3] == 0);
  __VERIFIER_assert(s[4] == 0);
  __VERIFIER_assert(i[0] == 11);
  __VERIFIER_assert(i[1] == 22);
  __VERIFIER_assert(i[2] == 0);
  __VERIFIER_assert(i[3] == 0);
  __VERIFIER_assert(i[4] == 0);
  __VERIFIER_assert(l[0] == 11);
  __VERIFIER_assert(l[1] == 22);
  __VERIFIER_assert(l[2] == 0);
  __VERIFIER_assert(l[3] == 0);
  __VERIFIER_assert(l[4] == 0);
  __VERIFIER_assert(ll[0] == 11);
  __VERIFIER_assert(ll[1] == 22);
  __VERIFIER_assert(ll[2] == 0);
  __VERIFIER_assert(ll[3] == 0);
  __VERIFIER_assert(ll[4] == 0);

  return 0;
}
