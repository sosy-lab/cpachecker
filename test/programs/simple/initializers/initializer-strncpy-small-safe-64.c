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

extern char *strncpy(char *dest, const char *src, unsigned long count);


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// Count one leaves no terminator; count two includes one zero byte.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char one[1] = {'Z'};
  char two[2] = {'Z', 'Z'};

  strncpy(one, "x", 1UL);
  strncpy(two, "x", 2UL);

  __VERIFIER_assert(one[0] == 'x');
  __VERIFIER_assert(two[0] == 'x');
  __VERIFIER_assert(two[1] == '\0');

  return 0;
}
