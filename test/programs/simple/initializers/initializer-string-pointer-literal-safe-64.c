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
// The pointer is initialized with the address of the string literal, which holds all
// characters including its terminator.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  const char *pointer = "abc";

  __VERIFIER_assert(sizeof(long) == 8);
  __VERIFIER_assert(sizeof(void *) == 8);
  __VERIFIER_assert(pointer[0] == 'a');
  __VERIFIER_assert(pointer[1] == 'b');
  __VERIFIER_assert(pointer[2] == 'c');
  __VERIFIER_assert(pointer[3] == '\0');

  return 0;
}
