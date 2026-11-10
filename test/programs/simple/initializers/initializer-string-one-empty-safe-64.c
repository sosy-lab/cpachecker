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
// The literal initializes its prefix; every remaining array element is zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char a[1] = "";

  __VERIFIER_assert(a[0] == '\0');

  return 0;
}
