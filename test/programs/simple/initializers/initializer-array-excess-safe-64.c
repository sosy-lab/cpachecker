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
// GCC retains each two-element array's size and values 11 and 22; excess 33 is discarded.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char c[2] = {11, 22, 33};
  short s[2] = {11, 22, 33};
  int i[2] = {11, 22, 33};
  long l[2] = {11, 22, 33};
  long long ll[2] = {11, 22, 33};

  __VERIFIER_assert(sizeof c == 2U * sizeof(char));
  __VERIFIER_assert(sizeof s == 2U * sizeof(short));
  __VERIFIER_assert(sizeof i == 2U * sizeof(int));
  __VERIFIER_assert(sizeof l == 2U * sizeof(long));
  __VERIFIER_assert(sizeof ll == 2U * sizeof(long long));
  __VERIFIER_assert(c[0] == 11);
  __VERIFIER_assert(c[1] == 22);
  __VERIFIER_assert(s[0] == 11);
  __VERIFIER_assert(s[1] == 22);
  __VERIFIER_assert(i[0] == 11);
  __VERIFIER_assert(i[1] == 22);
  __VERIFIER_assert(l[0] == 11);
  __VERIFIER_assert(l[1] == 22);
  __VERIFIER_assert(ll[0] == 11);
  __VERIFIER_assert(ll[1] == 22);

  return 0;
}
