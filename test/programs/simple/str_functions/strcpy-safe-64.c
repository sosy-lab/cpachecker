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
extern char *strcpy(char *dest, const char *src);

// Machine model: LP64. Expected: true for unreach-call and valid-memsafety.
// strcpy copies the source including its terminator and returns the destination;
// bytes after the copied terminator stay unchanged.
int main(void) {
  char exact[4] = {'Z', 'Z', 'Z', 'Z'};
  char larger[6] = {'Z', 'Z', 'Z', 'Z', 'Z', 'Z'};
  char source[3] = {'x', 'y', '\0'};

  char *exactResult = strcpy(exact, "abc");
  char *largerResult = strcpy(larger, source);

  __VERIFIER_assert(exactResult == exact);
  __VERIFIER_assert(exact[0] == 'a');
  __VERIFIER_assert(exact[1] == 'b');
  __VERIFIER_assert(exact[2] == 'c');
  __VERIFIER_assert(exact[3] == '\0');
  __VERIFIER_assert(largerResult == larger);
  __VERIFIER_assert(larger[0] == 'x');
  __VERIFIER_assert(larger[1] == 'y');
  __VERIFIER_assert(larger[2] == '\0');
  __VERIFIER_assert(larger[3] == 'Z');
  __VERIFIER_assert(larger[4] == 'Z');
  __VERIFIER_assert(larger[5] == 'Z');

  // The destination starts inside an array and the source lives on the heap.
  char *heap = malloc(3UL);
  if (heap == 0) {
    return 0;
  }
  strcpy(heap, "ab");
  char offset[5] = {'Z', 'Z', 'Z', 'Z', 'Z'};
  char *offsetResult = strcpy(offset + 2, heap);
  free(heap);

  __VERIFIER_assert(offsetResult == offset + 2);
  __VERIFIER_assert(offset[0] == 'Z');
  __VERIFIER_assert(offset[1] == 'Z');
  __VERIFIER_assert(offset[2] == 'a');
  __VERIFIER_assert(offset[3] == 'b');
  __VERIFIER_assert(offset[4] == '\0');

  return 0;
}
