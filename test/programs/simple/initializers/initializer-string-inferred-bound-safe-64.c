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
// The inferred bound includes the literal terminator; all fourteen elements are initialized.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char a[] = "abcdefghijklm";

  __VERIFIER_assert(sizeof a == 14UL);
  __VERIFIER_assert(a[0] == 'a');
  __VERIFIER_assert(a[1] == 'b');
  __VERIFIER_assert(a[2] == 'c');
  __VERIFIER_assert(a[3] == 'd');
  __VERIFIER_assert(a[4] == 'e');
  __VERIFIER_assert(a[5] == 'f');
  __VERIFIER_assert(a[6] == 'g');
  __VERIFIER_assert(a[7] == 'h');
  __VERIFIER_assert(a[8] == 'i');
  __VERIFIER_assert(a[9] == 'j');
  __VERIFIER_assert(a[10] == 'k');
  __VERIFIER_assert(a[11] == 'l');
  __VERIFIER_assert(a[12] == 'm');
  __VERIFIER_assert(a[13] == '\0');

  return 0;
}
