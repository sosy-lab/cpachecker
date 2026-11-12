// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *malloc(unsigned long size);
extern void free(void *ptr);
extern int snprintf(char *str, unsigned long size, const char *format, ...);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  char *src = malloc(3UL);
  if (src == 0) {
    return 0;
  }
  src[0] = 'a';
  src[1] = 'b';
  src[2] = '\0';
  free(src);
  char dest[4];

  // Invalid: the string argument for %s was freed before snprintf reads it.
  snprintf(dest, 4UL, "%s", src);

  return 0;
}
