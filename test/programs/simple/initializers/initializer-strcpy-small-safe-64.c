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

extern char *strcpy(char *dest, const char *src);


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// The empty string fits in one byte; x and its terminator fit in two bytes.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char one[1] = {'Z'};
  char two[2] = {'Z', 'Z'};

  strcpy(one, "");
  strcpy(two, "x");

  __VERIFIER_assert(one[0] == '\0');
  __VERIFIER_assert(two[0] == 'x');
  __VERIFIER_assert(two[1] == '\0');

  return 0;
}
