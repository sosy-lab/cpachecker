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
extern char *strncpy(char *dest, const char *src, unsigned long n);

// Machine model: LP64. Expected: true for unreach-call and valid-memsafety.
// strncpy writes exactly n bytes: it copies at most n characters, adds no terminator when
// truncating, and pads with zeros when the source is shorter than n.
int main(void) {
  char truncated[5] = {'Z', 'Z', 'Z', 'Z', 'Z'};
  char padded[6] = {'Z', 'Z', 'Z', 'Z', 'Z', 'Z'};
  char unchanged[2] = {'Z', 'Z'};

  char *truncatedResult = strncpy(truncated, "abcdef", 3UL);
  char *paddedResult = strncpy(padded, "ab", 5UL);
  char *unchangedResult = strncpy(unchanged, "ab", 0UL);

  __VERIFIER_assert(truncatedResult == truncated);
  __VERIFIER_assert(truncated[0] == 'a');
  __VERIFIER_assert(truncated[1] == 'b');
  __VERIFIER_assert(truncated[2] == 'c');
  __VERIFIER_assert(truncated[3] == 'Z');
  __VERIFIER_assert(truncated[4] == 'Z');
  __VERIFIER_assert(paddedResult == padded);
  __VERIFIER_assert(padded[0] == 'a');
  __VERIFIER_assert(padded[1] == 'b');
  __VERIFIER_assert(padded[2] == '\0');
  __VERIFIER_assert(padded[3] == '\0');
  __VERIFIER_assert(padded[4] == '\0');
  __VERIFIER_assert(padded[5] == 'Z');
  __VERIFIER_assert(unchangedResult == unchanged);
  __VERIFIER_assert(unchanged[0] == 'Z');
  __VERIFIER_assert(unchanged[1] == 'Z');

  // An unterminated source is valid if n does not exceed its size.
  char source[3] = {'x', 'y', 'z'};
  char bounded[4] = {'Z', 'Z', 'Z', 'Z'};
  strncpy(bounded, source, 3UL);

  __VERIFIER_assert(bounded[0] == 'x');
  __VERIFIER_assert(bounded[1] == 'y');
  __VERIFIER_assert(bounded[2] == 'z');
  __VERIFIER_assert(bounded[3] == 'Z');

  char *heap = malloc(4UL);
  if (heap == 0) {
    return 0;
  }
  strncpy(heap, "a", 4UL);

  __VERIFIER_assert(heap[0] == 'a');
  __VERIFIER_assert(heap[1] == '\0');
  __VERIFIER_assert(heap[2] == '\0');
  __VERIFIER_assert(heap[3] == '\0');

  free(heap);
  return 0;
}
