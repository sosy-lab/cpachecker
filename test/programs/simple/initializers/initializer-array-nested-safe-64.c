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
// Partially initialized rows and the entirely omitted final row receive zeros.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char c[3][3] = {{11}, {22}};
  short s[3][3] = {{11}, {22}};
  int i[3][3] = {{11}, {22}};
  long l[3][3] = {{11}, {22}};
  long long ll[3][3] = {{11}, {22}};

  __VERIFIER_assert(c[0][0] == 11);
  __VERIFIER_assert(c[0][1] == 0);
  __VERIFIER_assert(c[0][2] == 0);
  __VERIFIER_assert(c[1][0] == 22);
  __VERIFIER_assert(c[1][1] == 0);
  __VERIFIER_assert(c[1][2] == 0);
  __VERIFIER_assert(c[2][0] == 0);
  __VERIFIER_assert(c[2][1] == 0);
  __VERIFIER_assert(c[2][2] == 0);
  __VERIFIER_assert(s[0][0] == 11);
  __VERIFIER_assert(s[0][1] == 0);
  __VERIFIER_assert(s[0][2] == 0);
  __VERIFIER_assert(s[1][0] == 22);
  __VERIFIER_assert(s[1][1] == 0);
  __VERIFIER_assert(s[1][2] == 0);
  __VERIFIER_assert(s[2][0] == 0);
  __VERIFIER_assert(s[2][1] == 0);
  __VERIFIER_assert(s[2][2] == 0);
  __VERIFIER_assert(i[0][0] == 11);
  __VERIFIER_assert(i[0][1] == 0);
  __VERIFIER_assert(i[0][2] == 0);
  __VERIFIER_assert(i[1][0] == 22);
  __VERIFIER_assert(i[1][1] == 0);
  __VERIFIER_assert(i[1][2] == 0);
  __VERIFIER_assert(i[2][0] == 0);
  __VERIFIER_assert(i[2][1] == 0);
  __VERIFIER_assert(i[2][2] == 0);
  __VERIFIER_assert(l[0][0] == 11);
  __VERIFIER_assert(l[0][1] == 0);
  __VERIFIER_assert(l[0][2] == 0);
  __VERIFIER_assert(l[1][0] == 22);
  __VERIFIER_assert(l[1][1] == 0);
  __VERIFIER_assert(l[1][2] == 0);
  __VERIFIER_assert(l[2][0] == 0);
  __VERIFIER_assert(l[2][1] == 0);
  __VERIFIER_assert(l[2][2] == 0);
  __VERIFIER_assert(ll[0][0] == 11);
  __VERIFIER_assert(ll[0][1] == 0);
  __VERIFIER_assert(ll[0][2] == 0);
  __VERIFIER_assert(ll[1][0] == 22);
  __VERIFIER_assert(ll[1][1] == 0);
  __VERIFIER_assert(ll[1][2] == 0);
  __VERIFIER_assert(ll[2][0] == 0);
  __VERIFIER_assert(ll[2][1] == 0);
  __VERIFIER_assert(ll[2][2] == 0);

  return 0;
}
