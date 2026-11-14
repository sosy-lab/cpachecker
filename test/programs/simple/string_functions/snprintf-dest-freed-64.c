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
  char *dest = malloc(4UL);
  if (dest == 0) {
    return 0;
  }
  free(dest);

  // Invalid: str was freed before snprintf writes to it.
  snprintf(dest, 4UL, "%s", "ab");

  return 0;
}
