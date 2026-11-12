// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern char *strncpy(char *dest, const char *src, unsigned long n);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  char *null = 0;
  char dest[4];

  // Invalid: src is a null pointer.
  strncpy(dest, null, 4UL);

  return 0;
}
