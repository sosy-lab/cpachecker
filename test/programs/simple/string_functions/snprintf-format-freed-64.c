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
  char *format = malloc(3UL);
  if (format == 0) {
    return 0;
  }
  format[0] = '%';
  format[1] = 's';
  format[2] = '\0';
  free(format);
  char dest[4];

  // Invalid: format was freed before snprintf reads it.
  snprintf(dest, 4UL, format, "ab");

  return 0;
}
