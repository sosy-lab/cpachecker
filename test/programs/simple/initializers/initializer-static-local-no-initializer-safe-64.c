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
// The local arrays have static storage duration, so every element starts at zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  static char c[3];
  static short s[3];
  static int i[3];
  static long l[3];
  static long long ll[3];

  __VERIFIER_assert(c[0] == 0);
  __VERIFIER_assert(c[1] == 0);
  __VERIFIER_assert(c[2] == 0);
  __VERIFIER_assert(s[0] == 0);
  __VERIFIER_assert(s[1] == 0);
  __VERIFIER_assert(s[2] == 0);
  __VERIFIER_assert(i[0] == 0);
  __VERIFIER_assert(i[1] == 0);
  __VERIFIER_assert(i[2] == 0);
  __VERIFIER_assert(l[0] == 0);
  __VERIFIER_assert(l[1] == 0);
  __VERIFIER_assert(l[2] == 0);
  __VERIFIER_assert(ll[0] == 0);
  __VERIFIER_assert(ll[1] == 0);
  __VERIFIER_assert(ll[2] == 0);

  return 0;
}
