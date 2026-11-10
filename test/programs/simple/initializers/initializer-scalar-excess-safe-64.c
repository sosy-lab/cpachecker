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
// GCC diagnoses and discards 22; each scalar is initialized to 11.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char c = {11, 22};
  short s = {11, 22};
  int i = {11, 22};
  long l = {11, 22};
  long long ll = {11, 22};

  __VERIFIER_assert(c == 11);
  __VERIFIER_assert(s == 11);
  __VERIFIER_assert(i == 11);
  __VERIFIER_assert(l == 11);
  __VERIFIER_assert(ll == 11);

  return 0;
}
