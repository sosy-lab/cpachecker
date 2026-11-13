// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
extern void *malloc(unsigned long size);
extern void free(void *ptr);
extern int strcmp(const char *s1, const char *s2);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  char *s = malloc(2UL);
  if (s == 0) {
    return 0;
  }
  s[0] = 'a';
  s[1] = '\0';
  free(s);

  // Invalid: s was freed.
  int result = strcmp(s, "a");

  return result;
}
