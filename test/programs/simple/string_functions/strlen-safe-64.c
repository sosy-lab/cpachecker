// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error(void) __attribute__((__noreturn__));

void __VERIFIER_assert(int condition) {
  if (!condition) {
    __VERIFIER_error();
  }
}

extern void *malloc(unsigned long size);
extern void free(void *ptr);
extern unsigned long strlen(const char *s);

// Machine model: LP64. Expected: true for unreach-call and valid-memsafety.
// strlen counts the characters before the first terminator.
int main(void) {
  char embedded[6] = {'a', 'b', '\0', 'd', 'e', '\0'};
  char exact[3] = {'x', 'y', '\0'};

  __VERIFIER_assert(strlen("") == 0UL);
  __VERIFIER_assert(strlen("abc") == 3UL);
  __VERIFIER_assert(strlen(embedded) == 2UL);
  __VERIFIER_assert(strlen(embedded + 3) == 2UL);
  __VERIFIER_assert(strlen(exact) == 2UL);

  char *heap = malloc(4UL);
  if (heap == 0) {
    return 0;
  }
  heap[0] = 'a';
  heap[1] = 'b';
  heap[2] = 'c';
  heap[3] = '\0';

  __VERIFIER_assert(strlen(heap) == 3UL);
  __VERIFIER_assert(strlen(heap + 1) == 2UL);

  free(heap);
  return 0;
}
