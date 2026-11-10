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

extern void *malloc(unsigned long size);
extern void free(void *ptr);
extern void *memcpy(void *dest, const void *src, unsigned long count);


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// Both explicit counts copy exactly their requested bytes and preserve the seeded tail.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  // Subcase: memcpy-payload-tail.
  {
    char *p = malloc(19UL);
    if (p == 0) {
      return 0;
    }

    for (int i = 0; i < 19; ++i) {
      p[i] = 'Z';
    }

    memcpy(p, "abcdefghijklm", 13UL);

    __VERIFIER_assert(p[0] == 'a');
    __VERIFIER_assert(p[1] == 'b');
    __VERIFIER_assert(p[2] == 'c');
    __VERIFIER_assert(p[3] == 'd');
    __VERIFIER_assert(p[4] == 'e');
    __VERIFIER_assert(p[5] == 'f');
    __VERIFIER_assert(p[6] == 'g');
    __VERIFIER_assert(p[7] == 'h');
    __VERIFIER_assert(p[8] == 'i');
    __VERIFIER_assert(p[9] == 'j');
    __VERIFIER_assert(p[10] == 'k');
    __VERIFIER_assert(p[11] == 'l');
    __VERIFIER_assert(p[12] == 'm');
    __VERIFIER_assert(p[13] == 'Z');
    __VERIFIER_assert(p[14] == 'Z');
    __VERIFIER_assert(p[15] == 'Z');
    __VERIFIER_assert(p[16] == 'Z');
    __VERIFIER_assert(p[17] == 'Z');
    __VERIFIER_assert(p[18] == 'Z');

    free(p);
  }

  // Subcase: memcpy-terminated-tail.
  {
    char *p = malloc(19UL);
    if (p == 0) {
      return 0;
    }

    for (int i = 0; i < 19; ++i) {
      p[i] = 'Z';
    }

    memcpy(p, "abcdefghijklm", 14UL);

    __VERIFIER_assert(p[0] == 'a');
    __VERIFIER_assert(p[1] == 'b');
    __VERIFIER_assert(p[2] == 'c');
    __VERIFIER_assert(p[3] == 'd');
    __VERIFIER_assert(p[4] == 'e');
    __VERIFIER_assert(p[5] == 'f');
    __VERIFIER_assert(p[6] == 'g');
    __VERIFIER_assert(p[7] == 'h');
    __VERIFIER_assert(p[8] == 'i');
    __VERIFIER_assert(p[9] == 'j');
    __VERIFIER_assert(p[10] == 'k');
    __VERIFIER_assert(p[11] == 'l');
    __VERIFIER_assert(p[12] == 'm');
    __VERIFIER_assert(p[13] == '\0');
    __VERIFIER_assert(p[14] == 'Z');
    __VERIFIER_assert(p[15] == 'Z');
    __VERIFIER_assert(p[16] == 'Z');
    __VERIFIER_assert(p[17] == 'Z');
    __VERIFIER_assert(p[18] == 'Z');

    free(p);
  }

  return 0;
}
