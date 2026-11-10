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

extern void *memcpy(void *dest, const void *src, unsigned long count);


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// Only the explicitly copied byte changes; the second seeded byte stays Z.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char one[1] = {'Z'};
  char two[2] = {'Z', 'Z'};

  memcpy(one, "x", 1UL);
  memcpy(two, "x", 1UL);

  __VERIFIER_assert(one[0] == 'x');
  __VERIFIER_assert(two[0] == 'x');
  __VERIFIER_assert(two[1] == 'Z');

  return 0;
}
