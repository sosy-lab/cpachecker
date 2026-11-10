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
// Each scalar compound literal has the explicitly supplied value zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char c = (char){0};
  short s = (short){0};
  int i = (int){0};
  long l = (long){0};
  long long ll = (long long){0};

  __VERIFIER_assert(c == 0);
  __VERIFIER_assert(s == 0);
  __VERIFIER_assert(i == 0);
  __VERIFIER_assert(l == 0);
  __VERIFIER_assert(ll == 0);

  return 0;
}
