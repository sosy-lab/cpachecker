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
extern char *strncpy(char *dest, const char *src, unsigned long count);


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// strncpy pads all remaining positions through its count of nineteen with zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char *p = malloc(19UL);
  if (p == 0) {
    return 0;
  }

  for (int i = 0; i < 19; ++i) {
    p[i] = 'Z';
  }

  strncpy(p, "abcdefghijklm", 19UL);

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
  __VERIFIER_assert(p[14] == '\0');
  __VERIFIER_assert(p[15] == '\0');
  __VERIFIER_assert(p[16] == '\0');
  __VERIFIER_assert(p[17] == '\0');
  __VERIFIER_assert(p[18] == '\0');

  free(p);

  return 0;
}
